package ford.james.motorola.functions;

@FunctionalInterface
public interface LockFunction<R> {

	R apply() throws Exception;

}
