package fr.fst.michelin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "lucene.index=target/index-test/application")
class MichelinApplicationTests {

	@Test
	void contextLoads() {
	}

}
