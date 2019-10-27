package takamaka.blockchain;

import java.math.BigInteger;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;

/**
 * The signature of a method of a class.
 */
@Immutable
public abstract class MethodSignature extends CodeSignature {

	private static final long serialVersionUID = -1068494776407417852L;

	/**
	 * The name of the method.
	 */
	public final String methodName;

	/**
	 * Builds the signature of a method.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	protected MethodSignature(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, formals);

		this.methodName = methodName;
	}

	@Override
	public String toString() {
		return definingClass + "." + methodName + commaSeparatedFormals();
	};

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodSignature && methodName.equals(((MethodSignature) other).methodName) && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ methodName.hashCode();
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.storageCostOf(methodName));
	}
}