package mobicomp.emu;

/**
 * 
 * @author nic
 *
 * Dummy class to break up the delegation model of the classloaders.
 * As the system classloader cannot be reached from an URLclassloader using an instance of this class as parent 
 * each client can be granted its own namespace without the risk of unwanted hidden communication channels (e.g. by 
 * static variables).
 * 
 * THIS IS A HACK!
 * 
 */

 public class BlockerLoader extends ClassLoader
{
    protected BlockerLoader() {
        super(null);
    }

    protected Class findClass(String name)
        throws ClassNotFoundException {
    	throw new ClassNotFoundException();
    }

}
