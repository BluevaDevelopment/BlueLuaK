package org.luaj.vm2.lib.jme;

import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * Test-only stub for the JME platform.
 *
 * The real JME implementation depends on CLDC/MIDP APIs that are not available
 * in a standard Java 17 environment. This stub delegates to the JSE platform so
 * that tests referencing JmePlatform can compile and run during the Gradle
 * migration. It is intentionally not a faithful JME implementation.
 */
public class JmePlatform {

	public static Globals standardGlobals() {
		return JsePlatform.standardGlobals();
	}

	public static Globals debugGlobals() {
		return JsePlatform.debugGlobals();
	}
}
