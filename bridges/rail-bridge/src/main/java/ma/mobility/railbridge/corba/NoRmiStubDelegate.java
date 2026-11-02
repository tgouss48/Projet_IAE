package ma.mobility.railbridge.corba;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;

import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.StubDelegate;
import org.omg.CORBA.ORB;

// JacORB 3.9 charge javax.rmi.CORBA.Stub, qui n'existe plus depuis Java 11.
// On n'utilise pas RMI-IIOP donc ce delegate ne fait rien.
public class NoRmiStubDelegate implements StubDelegate {

    @Override
    public int hashCode(Stub self) {
        return System.identityHashCode(self);
    }

    @Override
    public boolean equals(Stub self, Object obj) {
        return self == obj;
    }

    @Override
    public String toString(Stub self) {
        return self.getClass().getName();
    }

    @Override
    public void connect(Stub self, ORB orb) throws RemoteException {
        throw new RemoteException("RMI-IIOP non supporte");
    }

    @Override
    public void readObject(Stub self, ObjectInputStream s) throws IOException {
        throw new IOException("RMI-IIOP non supporte");
    }

    @Override
    public void writeObject(Stub self, ObjectOutputStream s) throws IOException {
        throw new IOException("RMI-IIOP non supporte");
    }
}
