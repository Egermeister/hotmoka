module io.hotmoka.memory {
	exports io.hotmoka.memory;
	exports io.hotmoka.memory.runs;
	requires transitive io.takamaka.code.engine;
	requires io.hotmoka.nodes;
	requires org.slf4j;
	requires io.hotmoka.beans;
	requires io.takamaka.code.constants;
}