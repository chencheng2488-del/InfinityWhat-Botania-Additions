package com.bandybus.infwhat.botania.util;

import com.bandybus.infwhat.botania.InfinityWhatBotania;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Minimal reflection helper used for Botania's private members.
 *
 * <p>The 1.12.2 port has no mixin bootstrap, so the few places that must touch Botania internals
 * (the Gaia guardian's private spawn state and the altars' cooldown counter) use plain reflection.
 * Field and method names of mod classes are not obfuscated, so no SRG lookup is involved. A
 * missing member is a soft failure: the caller disables just that sub-feature.
 */
public final class Reflect {

	public static Field field(Class<?> owner, String name) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException | SecurityException exception) {
			InfinityWhatBotania.LOGGER.warn("Infinity What: Botania Additions could not access {}.{}",
					owner.getName(), name);
			return null;
		}
	}

	public static boolean set(Object target, Field field, Object value) {
		if (field == null || target == null) {
			return false;
		}
		try {
			field.set(target, value);
			return true;
		} catch (IllegalAccessException exception) {
			InfinityWhatBotania.LOGGER.warn(
					"Infinity What: Botania Additions could not write a reflected field", exception);
			return false;
		}
	}

	public static int getInt(Object target, Field field, int fallback) {
		if (field == null || target == null) {
			return fallback;
		}
		try {
			return field.getInt(target);
		} catch (IllegalAccessException exception) {
			return fallback;
		}
	}

	public static void setInt(Object target, Field field, int value) {
		if (field == null || target == null) {
			return;
		}
		try {
			field.setInt(target, value);
		} catch (IllegalAccessException exception) {
			InfinityWhatBotania.LOGGER.warn(
					"Infinity What: Botania Additions could not write a reflected int", exception);
		}
	}

	/**
	 * Finds a method by its parameter types only.
	 *
	 * <p>Vanilla methods are renamed to SRG names in a production runtime, so {@code onInitialSpawn}
	 * cannot be looked up by its MCP name. Mod classes keep their names, which is why the Botania
	 * field accessors above can use string names.
	 */
	public static Method methodByParameters(Class<?> owner, Class<?>... parameters) {
		Class<?> current = owner;
		while (current != null && current != Object.class) {
			for (Method method : current.getDeclaredMethods()) {
				if (java.util.Arrays.equals(method.getParameterTypes(), parameters)) {
					method.setAccessible(true);
					return method;
				}
			}
			current = current.getSuperclass();
		}
		InfinityWhatBotania.LOGGER.warn(
				"Infinity What: Botania Additions could not find a method on {}", owner.getName());
		return null;
	}

	public static Object invoke(Method method, Object target, Object... arguments) {
		if (method == null || target == null) {
			return null;
		}
		try {
			return method.invoke(target, arguments);
		} catch (ReflectiveOperationException exception) {
			InfinityWhatBotania.LOGGER.warn(
					"Infinity What: Botania Additions could not invoke a reflected method", exception);
			return null;
		}
	}

	private Reflect() {
	}
}
