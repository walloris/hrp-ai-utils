---
name: generate-tests
description: >
  Генерирует чистые юнит-тесты для Spring Boot 3 приложений на JUnit 5 и Mockito.
  Без БД, без контейнеров, без Spring-контекста в сервисных тестах.
  Покрывает контроллеры (@WebMvcTest, MockMvc), сервисы (Mockito), DTO (@JsonTest),
  обработчики ошибок (@ControllerAdvice).
  Также активировать при упоминании JUnit, Mockito, MockMvc, @WebMvcTest, @JsonTest.
---

# Spring Boot 3 Unit Testing Skill

Чистые юнит-тесты.

## Stack

- **Spring Boot 3**, **Java 17+**
- **JUnit 5** for assertions (`assertEquals`, `assertThrows`, `assertAll`, etc.)
- **Mockito 5.x** — classic style (`when`/`verify`), not BDDMockito
- `@MockitoBean` replaces the deprecated `@MockBean` (removed in Boot 4.0)

## Data-Access Mocking Style

Before writing service tests, determine what data-access layer the project uses — this affects only the mocking style:

| Signal | Stack | Mocking style |
|--------|-------|---------------|
| `@Mapper`, `@Select`, MyBatis imports | MyBatis | Returns raw values or `null`. Insert/update return `int` (affected rows) |
| `JpaRepository`, `@Entity`, Hibernate imports | JPA | Returns `Optional`. Save returns the entity |

Both stacks are mocked with `@Mock` — no database, no Spring context. See `references/service-tests.md` for patterns of both.

## What to Unit-Test

| Layer | Test style | Reference |
|-------|-----------|-----------|
| Controller | `@WebMvcTest` + MockMvc | `references/controller-tests.md` |
| Service | `@ExtendWith(MockitoExtension.class)` | `references/service-tests.md` |
| Exception handler | `@WebMvcTest` with thrown exceptions | `references/exception-handling-tests.md` |
| DTO serialization | `@JsonTest` | `references/controller-tests.md` (bottom) |
| Repository / Mapper | **Not tested directly** | Mocked as dependencies in service tests |

## Core Conventions

### File & Class Naming

| Kind | Suffix | Example |
|------|--------|---------|
| Unit test | `Test` | `OrderServiceTest` |
| Integration test | `IT` | `OrderServiceIT` |

### Method Naming

```
should[Action]_When[Condition]
```

Every test method and class carries `@DisplayName` with a plain-English sentence.

### Test Structure

Every test class uses `@Nested` to group related scenarios. Every test follows arrange / act / assert:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock private OrderMapper orderMapper;       // or OrderRepository
    @Mock private PaymentService paymentService;
    @InjectMocks private OrderServiceImpl orderService;

    @Captor private ArgumentCaptor<Order> orderCaptor;

    @Nested
    @DisplayName("Place Order")
    class PlaceOrderTests {

        @Test
        @DisplayName("Creates order when items are in stock")
        void shouldCreateOrder_WhenItemsInStock() {
            // arrange
            when(orderMapper.insert(any(Order.class))).thenReturn(1);

            // act
            var result = orderService.place(new PlaceOrderRequest(/*...*/));

            // assert
            assertNotNull(result);
            verify(orderMapper).insert(orderCaptor.capture());
            assertEquals("CREATED", orderCaptor.getValue().getStatus());
        }
    }
}
```

### Mockito Rules

Use classic Mockito only:

```java
// stubbing
when(dep.method(arg)).thenReturn(value);
when(dep.method(any(Type.class))).thenAnswer(inv -> inv.getArgument(0));

// verification
verify(dep).method(captor.capture());
verify(dep, times(2)).process(any());
verifyNoInteractions(unusedDep);
```

### Captors

Declare at class level with `@Captor` — never inline:

```java
@Captor private ArgumentCaptor<Order> orderCaptor;
@Captor private ArgumentCaptor<Set<UUID>> idsCaptor;
```

When a mock is called multiple times, capture all and filter:

```java
verify(repo, times(2)).save(orderCaptor.capture());
var root = orderCaptor.getAllValues().stream()
        .filter(o -> "ROOT".equals(o.getType()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Root order not found"));
assertEquals("PAID", root.getStatus());
```

### Assertions

Use JUnit 5 assertions. Use `assertAll` when checking multiple properties of one result to avoid early exit:

```java
assertAll(
    () -> assertEquals("Alice", dto.getName()),
        () -> assertEquals("alice@test.com", dto.getEmail()),
        () -> assertNotNull(dto.getCreatedAt())
        );

assertThrows(ResourceNotFoundException.class,
    () -> service.findById(unknownId));
```

To find a specific item in a list:

```java
var item = list.stream()
        .filter(e -> "target".equals(e.getCode()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected item not found"));
```

### Parameterized Tests

Use `@ParameterizedTest` for repetitive value-driven scenarios:

```java
@ParameterizedTest
@CsvSource({"100, 0, 100.00", "100, 10, 90.00", "100, 100, 0.00"})
@DisplayName("Applies discount correctly")
void shouldApplyDiscount(BigDecimal price, int pct, BigDecimal expected) {
    assertEquals(expected, pricingService.applyDiscount(price, pct));
}

@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = {"  ", "\t"})
@DisplayName("Rejects blank name")
void shouldRejectBlankName(String name) { /*...*/ }

@ParameterizedTest
@MethodSource("invalidEmails")
@DisplayName("Rejects invalid email")
void shouldReject_InvalidEmail(String email) { /*...*/ }

static Stream<String> invalidEmails() {
    return Stream.of(null, "", "  ", "no-at", "@no-local");
}
```

### Fixed Clock for Time-Dependent Logic

Inject `Clock` into services. In tests, pin it:

```java
private final Clock fixedClock =
        Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneOffset.UTC);

@BeforeEach
void setUp() {
    service = new SubscriptionService(mapper, fixedClock);
}
```

## Anti-Patterns — Do Not

| Anti-pattern | Why | Correct approach |
|-------------|-----|-----------------|
| `@MockBean` | Deprecated in Boot 3.4, removed in 4.0 | `@MockitoBean` (Spring context) or `@Mock` (plain unit) |
| BDDMockito (`given`/`then`) | Inconsistent with team style | Classic `when`/`verify` |
| Missing `@DisplayName` | Unreadable test reports | Required on every class and method |
| Flat test class (no `@Nested`) | Hard to navigate | Group by scenario / endpoint |
| `ArgumentCaptor.forClass(...)` inline | Verbose and error-prone | `@Captor` at class level |
| `@SpringBootTest` for service tests | Loads full context, slow | `@ExtendWith(MockitoExtension.class)` |
| `@DataJpaTest` / `@MybatisTest` | Requires DB, not unit test | Mock repositories/mappers with `@Mock` |
| `Thread.sleep()` for async | Flaky and slow | Awaitility (`await().atMost(...)`) |
| Testing SQL in service test | Wrong concern | Service tests verify business logic, not queries |

## References

Read the reference that matches the layer you are testing:

- `references/controller-tests.md` — `@WebMvcTest`, MockMvc, `@JsonTest`
- `references/service-tests.md` — plain Mockito unit tests for business logic (JPA and MyBatis mocking patterns)
- `references/exception-handling-tests.md` — testing `@ControllerAdvice` / `@RestControllerAdvice`