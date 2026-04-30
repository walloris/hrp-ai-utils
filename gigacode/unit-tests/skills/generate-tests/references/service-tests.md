# Service Tests Reference

Plain unit tests — no Spring context. These should form the bulk of test coverage because they are fast and focused on business logic.

## Setup

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;   // or UserMapper for MyBatis
    @Mock private NotificationService notificationService;
    @InjectMocks private UserServiceImpl userService;

    @Captor private ArgumentCaptor<User> userCaptor;

    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
    }
}
```

The data-access dependency is mocked regardless of whether it is a JPA repository or a MyBatis mapper. Service tests never care about SQL — they test business logic only.

## Mocking Data Access

### Return entity or null

```java
when(userRepository.findById(testId)).thenReturn(Optional.of(new User(testId, "Alice")));
when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
```

For MyBatis mappers that return `null` instead of `Optional`:

```java
when(userMapper.selectById(testId)).thenReturn(new User(testId, "Alice"));
when(userMapper.selectById(unknownId)).thenReturn(null);
```

### Return list

```java
when(userRepository.findByStatus("ACTIVE"))
    .thenReturn(List.of(new User(id1, "Alice"), new User(id2, "Bob")));
```

### Save that returns the same object

```java
when(userRepository.save(any(User.class)))
    .thenAnswer(inv -> inv.getArgument(0));
```

### Insert with captor verification

```java
when(userMapper.insert(any(User.class))).thenReturn(1);

userService.create(new CreateUserRequest("Alice", "alice@test.com"));

verify(userMapper).insert(userCaptor.capture());
assertEquals("Alice", userCaptor.getValue().getName());
```

### Auto-generated ID (MyBatis pattern)

```java
when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
    inv.getArgument(0, User.class).setId(1L);
    return 1;
});
```

## Nested Structure

Group by business operation:

```java
@Nested
@DisplayName("Create User")
class CreateUserTests {

    @Test
    @DisplayName("Saves user and sends welcome notification")
    void shouldSaveAndNotify_WhenValidRequest() {
        when(userRepository.save(any(User.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        var result = userService.create(
            new CreateUserRequest("Alice", "alice@test.com"));

        assertAll(
            () -> assertEquals("Alice", result.getName()),
            () -> verify(notificationService).sendWelcome(any())
        );
    }

    @Test
    @DisplayName("Throws when email already exists")
    void shouldThrow_WhenEmailExists() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
            () -> userService.create(
                new CreateUserRequest("Bob", "dup@test.com")));

        verify(userRepository, never()).save(any());
    }
}

@Nested
@DisplayName("Find User")
class FindUserTests {

    @Test
    @DisplayName("Returns user when found")
    void shouldReturnUser_WhenFound() {
        when(userRepository.findById(testId))
            .thenReturn(Optional.of(new User(testId, "Alice")));

        var result = userService.findById(testId);

        assertEquals("Alice", result.getName());
    }

    @Test
    @DisplayName("Throws when user not found")
    void shouldThrow_WhenNotFound() {
        when(userRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> userService.findById(testId));
    }
}
```

## Side Effects & Negative Paths

Verify that side effects do NOT happen when the main operation fails:

```java
@Test
@DisplayName("Does not send notification when save fails")
void shouldNotNotify_WhenSaveFails() {
    when(userRepository.save(any()))
        .thenThrow(new DataIntegrityViolationException("dup"));

    assertThrows(DataIntegrityViolationException.class,
        () -> userService.create(new CreateUserRequest("Alice", "a@b.com")));

    verifyNoInteractions(notificationService);
}
```

## Parameterized Tests

```java
@ParameterizedTest
@CsvSource({"100, 0, 100.00", "100, 10, 90.00", "100, 100, 0.00"})
@DisplayName("Applies discount correctly")
void shouldApplyDiscount(BigDecimal price, int pct, BigDecimal expected) {
    assertEquals(expected, pricingService.applyDiscount(price, pct));
}

@ParameterizedTest
@MethodSource("invalidEmails")
@DisplayName("Rejects invalid email")
void shouldReject_InvalidEmail(String email) {
    assertThrows(ValidationException.class,
        () -> userService.create(new CreateUserRequest("Alice", email)));
}

static Stream<String> invalidEmails() {
    return Stream.of(null, "", "  ", "no-at", "@no-local");
}
```

## Fixed Clock

For time-dependent logic, inject `Clock` into the service and pin it in tests:

```java
private final Clock fixedClock =
    Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneOffset.UTC);

@BeforeEach
void setUp() {
    service = new SubscriptionService(subscriptionMapper, fixedClock);
}

@Test
@DisplayName("Marks subscription as expired when past due date")
void shouldMarkExpired_WhenPastDue() {
    var sub = new Subscription();
    sub.setExpiresAt(Instant.parse("2025-01-14T00:00:00Z"));

    when(subscriptionMapper.selectById(1L)).thenReturn(sub);

    var result = service.checkStatus(1L);

    assertEquals("EXPIRED", result.getStatus());
}
```

## Tips

- Service tests should be the majority of your test suite — they are the fastest and most focused.
- Mock at the boundary: data access, external clients, messaging. Test business logic in the middle.
- One assertion concept per test. `assertAll` groups multiple checks on the same result, not unrelated assertions.
- If a service method does nothing but delegate to the repository, a test adds little value — focus on methods with branching logic.