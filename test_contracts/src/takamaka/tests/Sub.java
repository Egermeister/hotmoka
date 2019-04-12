package takamaka.tests;

import java.math.BigInteger;

import takamaka.lang.Entry;
import takamaka.lang.Payable;

public class Sub extends Super {

	public Sub() {
		super(13);
	}

	public @Entry @Payable Sub(int amount) {
		super(amount > 10 ? 13 : 17); // ok
	}

	@Override @Entry
	public void m1() { // this is implicitly @Entry by inheritance
		super.m1(); // exception at run time
		System.out.println("Sub.m1");
	}

	@Override @Entry
	public void m3() {
		System.out.println("Sub.m3 with caller " + caller());
	}

	@Override @Payable @Entry
	public String m4(int amount) {
		return "Sub.m4 receives " + amount + " coins from " + caller();
	}

	@Override @Payable @Entry
	public String m4_1(long amount) {
		return "Sub.m4_1 receives " + amount + " coins from " + caller();
	}

	@Override @Payable @Entry
	public String m4_2(BigInteger amount) {
		return "Sub.m4_2 receives " + amount + " coins from " + caller();
	}

	public void m5() {
		super.m2(); // ok
		new Sub(13);
	}

	public static void ms() {}
}