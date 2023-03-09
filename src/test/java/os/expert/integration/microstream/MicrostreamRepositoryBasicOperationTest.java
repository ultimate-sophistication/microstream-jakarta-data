package os.expert.integration.microstream;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The Microstream's PageableRepository default methods")
public class MicrostreamRepositoryBasicOperationTest {


    private MicrostreamRepository<Book, String> library;

    private DataStructure data;

    private MicrostreamTemplate template;

    private EntityMetadata metadata;

    @BeforeEach
    public void setUp() {
        this.metadata = EntityMetadata.of(Book.class);
        this.data = new DataStructure();
        this.template = new MicrostreamTemplate(data, metadata);
        this.library = new MicrostreamRepository<>(template);
    }

    @ParameterizedTest
    @ArgumentsSource(BookArgumentProvider.class)
    public void shouldSaveWhenDataDoesNotExist(Book book) {
        assertThat(data.isEmpty()).isTrue();
        this.library.save(book);
        assertThat(data.isEmpty()).isFalse();
    }

    @Test
    public void shouldReturnErrorWhenSaveIsNull() {
        assertThrows(NullPointerException.class, () -> this.library.save(null));
    }

    @ParameterizedTest
    @ArgumentsSource(BookArgumentProvider.class)
    public void shouldSaveWhenDataExist(Book book) {
        assertThat(data.isEmpty()).isTrue();
        this.library.save(book);
        this.library.save(book);
        assertThat(data.isEmpty()).isFalse();
        assertThat(data.size()).isEqualTo(1);
    }

    @ParameterizedTest
    @ArgumentsSource(BooksArgumentProvider.class)
    public void shouldSaveWhenDataDoesNotExist(List<Book> books) {
        assertThat(data.isEmpty()).isTrue();
        this.library.saveAll(books);
        assertThat(data.isEmpty()).isFalse();
        assertThat(data.size()).isEqualTo(books.size());
    }

    @Test
    public void shouldReturnErrorWhenSaveAllIsNull() {
        assertThrows(NullPointerException.class, () -> this.library.saveAll(null));
    }

    @ParameterizedTest
    @ArgumentsSource(BooksArgumentProvider.class)
    public void shouldSaveWhenDataExist(List<Book> books) {
        assertThat(data.isEmpty()).isTrue();
        this.library.saveAll(books);
        this.library.saveAll(books);
        assertThat(data.isEmpty()).isFalse();
        assertThat(data.size()).isEqualTo(books.size());
    }

    @ParameterizedTest
    @ArgumentsSource(BookArgumentProvider.class)
    public void shouldFindById(Book book) {
        this.library.save(book);
        Optional<Book> result = this.library.findById(book.isbn());
        assertThat(result)
                .isPresent()
                .contains(book);
    }

    @ParameterizedTest
    @ArgumentsSource(BookArgumentProvider.class)
    public void shouldNotFindById(Book book) {
        Optional<Book> result = this.library.findById(book.isbn());
        assertThat(result).isNotPresent();
    }

    @Test
    public void shouldReturnErrorFindById() {
        assertThrows(NullPointerException.class, () -> this.library.findById(null));
    }

    @ParameterizedTest
    @ArgumentsSource(BookArgumentProvider.class)
    public void shouldExistById(Book book) {
        this.library.save(book);
        assertThat(this.library.existsById(book.isbn())).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(BookArgumentProvider.class)
    public void shouldFalseExistById(Book book) {
        assertThat(this.library.existsById(book.isbn())).isFalse();
    }

    @Test
    public void shouldReturnErrorExistsById() {
        assertThrows(NullPointerException.class, () -> this.library.existsById(null));
    }


    @ParameterizedTest
    @ArgumentsSource(BooksArgumentProvider.class)
    public void shouldNotFindByAllId(List<Book> books) {
        this.library.saveAll(books);

        Stream<Book> found = this.library.findAllById(books.stream().map(Book::isbn)
                .collect(toUnmodifiableList()));

        assertThat(found)
                .hasSize(3)
                .containsAll(books);
    }

    @ParameterizedTest
    @ArgumentsSource(BooksArgumentProvider.class)
    public void shouldNotFindByAllIdWhereThereIsInvalidIds(List<Book> books) {

        List<String> ids = new ArrayList<>();
        ids.add("invalid");
        ids.add("invalid-2");
        books.stream().map(Book::isbn).forEach(ids::add);

        this.library.saveAll(books);
        Stream<Book> found = this.library.findAllById(books.stream().map(Book::isbn)
                .collect(toUnmodifiableList()));

        assertThat(found)
                .hasSize(3)
                .containsAll(books);
    }

    @Test
    public void shouldReturnErrorFindAllById() {
        assertThrows(NullPointerException.class, () -> this.library.findAllById( null));
    }


}
