# Exception Handling Tests Reference

Tests for `@ControllerAdvice` / `@RestControllerAdvice` classes. These verify that exceptions thrown by services are translated into correct HTTP responses with proper status codes and error bodies.

## Strategy

`@WebMvcTest` already loads `@ControllerAdvice` beans. To test exception handling, stub the service to throw, then assert on the HTTP response. No separate test context needed.

## Setup

Use any controller that depends on a service capable of throwing the relevant exceptions:

```java
@WebMvcTest(UserController.class)
@DisplayName("User Error Handling Tests")
class UserErrorHandlingTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;
}
```

## Testing Standard Exception Mappings

Assume a `@RestControllerAdvice` like:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
        return new ErrorResponse("VALIDATION_ERROR", String.join("; ", errors));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex) {
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
    }
}
```

Tests:

```java
@Nested
@DisplayName("ResourceNotFoundException Handling")
class NotFoundTests {

    @Test
    @DisplayName("Returns 404 with error body when resource not found")
    void shouldReturn404WithBody_WhenResourceNotFound() throws Exception {
        when(userService.findById(99L))
            .thenThrow(new ResourceNotFoundException("User 99 not found"));

        mockMvc.perform(get("/api/users/{id}", 99L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User 99 not found"));
    }
}

@Nested
@DisplayName("Validation Error Handling")
class ValidationTests {

    @Test
    @DisplayName("Returns 400 with field errors for invalid payload")
    void shouldReturn400WithFieldErrors_WhenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "", "email": "not-an-email"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

@Nested
@DisplayName("Unexpected Error Handling")
class UnexpectedErrorTests {

    @Test
    @DisplayName("Returns 500 with generic message for unexpected errors")
    void shouldReturn500_WhenUnexpectedError() throws Exception {
        when(userService.findById(1L))
            .thenThrow(new RuntimeException("DB connection lost"));

        mockMvc.perform(get("/api/users/{id}", 1L))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Does not leak internal details in 500 response")
    void shouldNotLeakInternalDetails() throws Exception {
        when(userService.findById(1L))
            .thenThrow(new RuntimeException("SQL syntax error near 'SELECT'"));

        mockMvc.perform(get("/api/users/{id}", 1L))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        // internal message must NOT appear in the response body
    }
}
```

## Custom Business Exceptions

If the project has domain-specific exceptions with custom status codes:

```java
@Nested
@DisplayName("InsufficientBalanceException Handling")
class InsufficientBalanceTests {

    @Test
    @DisplayName("Returns 422 when balance is insufficient")
    void shouldReturn422_WhenInsufficientBalance() throws Exception {
        when(orderService.place(any()))
            .thenThrow(new InsufficientBalanceException("Balance too low"));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrder())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }
}
```

## Tips

- Do not create a separate test class for `GlobalExceptionHandler` in isolation — test it through real controller endpoints so the full Spring MVC pipeline (argument resolution, validation, serialization) is exercised.
- Every `@ExceptionHandler` method should have at least one test proving the status code and response body shape.
- Always verify that 500 responses do not leak stack traces or internal SQL details.
- If the advice class handles multiple exception types, group tests with `@Nested` per exception type.