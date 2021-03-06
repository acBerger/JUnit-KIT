package test.framework;

import java.security.Permission;

/**
 * Used to catch when the code is trying to call {@link System#exit}. This would leave a test in a hanging state. A
 * {@link ExitException} is thrown if the code tries to call {@link System#exit}.
 * 
 * @author Joshua Gleitze
 *
 */
public class NoExitSecurityManager extends SecurityManager {
    private Class<?> targetClass;

    /**
     * Constructs a {@code NoExitSecurityManager}
     * 
     * @param targetClass
     *            The class that should be watched for calling {@code System.exit}
     */
    public NoExitSecurityManager(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public void checkExit(int status) {
        super.checkExit(status);
        Class<?>[] classContext = this.getClassContext();
        for (Class<?> c : classContext) {
            if (c == targetClass) {
                throw new ExitException(status);
            }
        }
    }

    @Override
    public void checkPermission(Permission perm) {
        // allow anything.
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        // allow anything.
    }
}