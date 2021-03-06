package test.framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import test.TestObject;
import test.mocking.MockerJavaClassFile;

/**
 * A parent last class loader to load the classes under test. This class loader is child first: It assures that any
 * class referenced by a class once loaded with this class loader will be loaded with this class loader. Therefore, a
 * new {@code TestClassLoader} assures to provide completly "fresh" classes. Classes can as well be mocked through
 * {@code #mock(MockerJavaClassFile)}.
 * 
 * @author Joshua Gleitze
 * @version 1.3
 * @since 24.01.2015
 *
 */
public class TestClassLoader extends ClassLoader {
    private static Map<String, byte[]> knownClasses = new HashMap<>();

    public TestClassLoader() {
        super();
    }

    @Override
    public URL getResource(String name) {
        return super.getResource(name);
    }

    /**
     * Replace a class with another one. When a tested class requires {@code mocker.getName()}, it will get the
     * implementation provided through {@code mocker.getByteCode()}.<br>
     * If the class is already loaded, it will <b>not</b> be replaced. All future instances of this class loader will
     * use {@code mocker}, though. If you are working with {@link TestObject}, call {@link TestObject#resetClass()}
     * after mocking to make sure the mocker will be loaded.<br>
     * To remove mocking, call {@link #forget(String)}.
     * 
     * @param mocker
     *            the {@link MockerJavaClassFile} object
     */
    public static void mock(MockerJavaClassFile mocker) {
        knownClasses.put(mocker.getName(), mocker.getByteCode());
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // respect the java.* packages.
        if (name.startsWith("java.")) {
            return super.loadClass(name, resolve);
        } else {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            byte[] classData = null;
            if (knownClasses.containsKey(name)) {
                classData = knownClasses.get(name);
            } else {
                String classPath;
                classPath = name.replace(".", System.getProperty("file.separator")) + ".class";
                InputStream in = getParent().getResourceAsStream(classPath);
                if (in == null) {
                    throw new ClassNotFoundException("There is no " + name + ". Are you sure about this class path?");
                }
                try {
                    classData = inputStreamToByteArray(in);
                } catch (IOException e) {
                    throw new ClassNotFoundException("Error reading " + name);
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new ClassNotFoundException("Error closing " + name + "' stream");
                    }
                }
                knownClasses.put(name, classData);
            }
            c = defineClass(name, classData, 0, classData.length);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int data = inputStream.read();
        while (data != -1) {
            buffer.write(data);
            data = inputStream.read();
        }
        return buffer.toByteArray();
    }

    /**
     * Makes this class loader forget about the bytecode of {@code className}. Guarantees that the class will be loaded
     * newly on all future instances of this class loader. Use this method to remove the mocking of classes.<br>
     * NOTE: Calling this method will not reset the currently loaded class! If you are working with {@link TestObject},
     * call {@link TestObject#resetClass()} after calling {@code forget} to make sure the original class will be loaded.
     * 
     * @param className
     *            The full qualified name of the class you want to reset.
     */
    public static void forget(String className) {
        knownClasses.remove(className);
    }
}