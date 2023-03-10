package os.expert.integration.microstream;

import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.PageableRepository;
import org.eclipse.jnosql.communication.query.QueryCondition;
import org.eclipse.jnosql.communication.query.QueryValue;
import org.eclipse.jnosql.communication.query.SelectQuery;
import org.eclipse.jnosql.communication.query.ValueType;
import org.eclipse.jnosql.communication.query.Where;
import org.eclipse.jnosql.communication.query.method.SelectMethodProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static os.expert.integration.microstream.CompareCondition.of;

class RepositoryProxy<T, K> implements InvocationHandler {

    private final PageableRepository<T, K> repository;

    private final MicrostreamTemplate template;

    RepositoryProxy(PageableRepository<T, K> repository, MicrostreamTemplate template) {
        this.repository = repository;
        this.template = template;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        RepositoryType type = RepositoryType.of(method);
        switch (type) {
            case DEFAULT:
                return method.invoke(repository, params);
            case FIND_BY:
                EntityMetadata metadata = template.metadata();
                SelectMethodProvider provider = SelectMethodProvider.INSTANCE;
                SelectQuery query = provider.apply(method, "");
                Predicate<T> predicate = query
                        .where()
                        .map(w -> {
                            Predicate<T> p = predicate(w, method, params, metadata);
                            return p;
                        }).orElse(null);


            case ORDER_BY:
            case COUNT_BY:
            case EXISTS_BY:
            case FIND_ALL:
            case DELETE_BY:
            case QUERY:
            default:
                throw new MappingException("There is not support for Microstream for feature of the type: " + type);
            case OBJECT_METHOD:
                return method.invoke(this, params);
        }
    }

    private <T> Predicate<T> predicate(Where where, Method method, Object[] params, EntityMetadata metadata) {
        QueryCondition condition = where.condition();
        FieldMetadata field = metadata.field(condition.name())
                .orElseThrow(() -> new MappingException("The the entity " + metadata.type() + " " +
                        "there is no field with the name: " + condition.name()));
        QueryValue<?> value = condition.value();
        AtomicInteger paramIndex = new AtomicInteger(0);
        Predicate<T> predicate = condition(condition, field, method, params, value, paramIndex);
        return predicate;
    }


    private static <T> Predicate<T> condition(QueryCondition condition, FieldMetadata field, Method method,
                                              Object[] params, QueryValue<?> value, AtomicInteger paramIndex) {

        Object param = param(method, params, value, paramIndex);

        switch (condition.condition()) {
            case EQUALS:
                return t -> param.equals(field.get(t));
            case GREATER_THAN:
                return of(param.getClass()).greater(param, field);
            case GREATER_EQUALS_THAN:
                return of(param.getClass()).greaterEquals(param, field);
            case LESSER_THAN:
                return of(param.getClass()).lesser(param, field);
            case LESSER_EQUALS_THAN:
                return of(param.getClass()).lesserEquals(param, field);
            case IN:
            case AND:
            case OR:
            case NOT:
            case LIKE:
            case BETWEEN:
            default:
                throw new UnsupportedOperationException("There is no support to method query using the condition: "
                        + condition.condition());


        }
    }

    private static Object param(Method method, Object[] params, QueryValue<?> value, AtomicInteger paramIndex) {

        if (value.type().equals(ValueType.PARAMETER)) {
            if (paramIndex.get() > params.length - 1) {
                throw new MappingException("There is arguments missing at the method repository: "
                        + method);
            }
            return requireNonNull(params[paramIndex.getAndIncrement()], "parameter cannot be null at repository");
        } else {
            return value.get();
        }
    }
}
