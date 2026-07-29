package net.blueva.luak.compiler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import net.blueva.luak.LuaValue;
import net.blueva.luak.parser.LuaParser;

public class LuaParserTests extends CompilerUnitTests {

    protected void setUp() throws Exception {
        super.setUp();
        LuaValue.valueOf(true);
    }
	
	protected void doTest(String file) {
		try {
			InputStream is = inputStreamOfFile(file);
			Reader r = new InputStreamReader(is, "ISO-8859-1");
			LuaParser parser = new LuaParser(r);
			parser.Chunk();
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}
}
