# Controller Tests Reference

Unit tests for the web layer using `@WebMvcTest`. Spring loads only the controller under test,
Jackson, validation, and exception handling — no services, no database.

## Setup

```java
@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserService userService;   // NOT @MockBean
}
```

Mock every service the controller depends on with `@MockitoBean`.

## Grouping

Group tests by endpoint using `@Nested`:

```java
@Nested
@DisplayName("GET /api/users/{id}")
class GetUserTests { /*...*/ }

@Nested
@DisplayName("POST /api/users")
class CreateUserTests { /*...*/ }

@Nested
@DisplayName("DELETE /api/users/{id}")
class DeleteUserTests { /*...*/ }
```

## GET — Happy & Error Paths

```java
@Test
@DisplayName("Returns 200 OK with user when valid ID provided")
void shouldReturnUser_WhenValidId() throws Exception {
    when(userService.findById(1L)).thenReturn(new UserDto(1L, "Alice"));

    mockMvc.perform(get("/api/users/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Alice"));
}

@Test
@DisplayName("Returns 404 Not Found when user does not exist")
void shouldReturn404_WhenNotFound() throws Exception {
    when(userService.findById(99L))
        .thenThrow(new ResourceNotFoundException("User not found"));

    mockMvc.perform(get("/api/users/{id}", 99L))
        .andExpect(status().isNotFound());
}
```

## POST

```java
@Test
@DisplayName("Returns 201 Created when payload is valid")
void shouldCreateUser_WhenValidPayload() throws Exception {
    var request = new CreateUserRequest("Alice", "alice@test.com");
    when(userService.create(any())).thenReturn(new UserDto(1L, "Alice"));

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1));
}

@Test
@DisplayName("Returns 400 Bad Request when email is invalid")
void shouldReturn400_WhenEmailInvalid() throws Exception {
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new CreateUserRequest("Alice", "not-an-email"))))
        .andExpect(status().isBadRequest());
}
```

## PUT / PATCH

```java
@Test
@DisplayName("Returns 200 OK with updated user when payload is valid")
void shouldUpdateUser_WhenValidPayload() throws Exception {
    var request = new UpdateUserRequest("Bob", "bob@test.com");
    when(userService.update(eq(1L), any())).thenReturn(new UserDto(1L, "Bob"));

    mockMvc.perform(put("/api/users/{id}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Bob"));
}
```

## DELETE

```java
@Test
@DisplayName("Returns 204 No Content when deleted")
void shouldReturn204_WhenDeleted() throws Exception {
    doNothing().when(userService).deleteById(1L);

    mockMvc.perform(delete("/api/users/{id}", 1L))
        .andExpect(status().isNoContent());
}
```

## Direct Controller Calls

When calling the controller method directly (without MockMvc), always use `HttpStatus` enum
constants — never raw `int` values. `response.getStatusCode()` returns `HttpStatusCode`,
which is not comparable to `int`.

```java
// correct
assertEquals(HttpStatus.ACCEPTED,   response.getStatusCode());
assertEquals(HttpStatus.CREATED,    response.getStatusCode());
assertEquals(HttpStatus.OK,         response.getStatusCode());
assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

// wrong — won't compile: int vs HttpStatusCode
assertEquals(202, response.getStatusCode());

// wrong — magic number, avoid even if it compiles
assertEquals(202, response.getStatusCode().value());
```

## Validation — Parameterized

```java
@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = {"  ", "\t"})
@DisplayName("Returns 400 Bad Request when name is blank")
void shouldRejectBlankName(String name) throws Exception {
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new CreateUserRequest(name, "valid@test.com"))))
        .andExpect(status().isBadRequest());
}
```

## Security-Protected Endpoints

```java
@WebMvcTest(AdminController.class)
@ImportAutoConfiguration(SecurityAutoConfiguration.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminService adminService;

    @Test @WithMockUser(roles = "ADMIN")
    @DisplayName("Returns 200 OK when user has ADMIN role")
    void shouldAllow_WhenAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    @DisplayName("Returns 403 Forbidden when user lacks ADMIN role")
    void shouldReturn403_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Returns 401 Unauthorized when unauthenticated")
    void shouldReturn401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isUnauthorized());
    }
}
```

## @JsonTest — DTO Serialization

```java
@JsonTest
@DisplayName("UserDto JSON Tests")
class UserDtoJsonTest {

    @Autowired private JacksonTester<UserDto> json;

    @Test
    @DisplayName("Serializes to JSON correctly")
    void shouldSerialize() throws Exception {
        var dto = new UserDto(1L, "Alice");

        assertThat(json.write(dto))
            .extractingJsonPathStringValue("$.name")
            .isEqualTo("Alice");
    }

    @Test
    @DisplayName("Deserializes from JSON correctly")
    void shouldDeserialize() throws Exception {
        var result = json.parse("""
            {"id":1,"name":"Alice"}
            """);

        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(new UserDto(1L, "Alice"));
    }
}
```

`JacksonTester` is AssertJ-based — AssertJ assertions are acceptable here.

## Tips

- Never use raw status codes (`202`, `404`) in assertions — always use `HttpStatus` enum constants.
- `andDo(print())` is useful for debugging — remove before committing.
- Always test both happy path and error path for every endpoint.
- Exception handling is tested via `@ControllerAdvice` — see `references/exception-handling-tests.md`.