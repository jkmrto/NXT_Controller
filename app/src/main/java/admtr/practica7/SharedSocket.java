package admtr.practica7;

import java.util.UUID;


class SharedSocket
// protect concurrent accesses to the shared socket
{
    protected android.bluetooth.BluetoothSocket socket;
    protected java.io.InputStream myinputstream;
    protected java.io.OutputStream myoutputstream;
    protected android.bluetooth.BluetoothDevice legodev;
    protected int oldlen;

    public SharedSocket()
    {
        this.socket=null;
    }

    synchronized public String create(android.bluetooth.BluetoothDevice dev)
    {
        this.legodev=dev;
        try {
            this.socket = this.legodev.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // en la prueba, 10 ms
            // the UUID is the type of profile that the server is expecting to serve, in this case, plain serial comms
            if (this.socket == null) return ("Couldn't create socket object");
        }
        catch (Exception e) { this.socket=null; return("EXCEPTION: ["+e.toString()+"]"); }
        return("");
    }

    synchronized public String connect()
    // block until connected or error
    {
        if ((this.socket!=null)&&(!this.socket.isConnected()))
            try
            {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                this.socket.connect(); // *** en la prueba, 3.5 segs
                this.myinputstream=this.socket.getInputStream();
                this.myoutputstream=this.socket.getOutputStream();
                this.oldlen=-1;
            }
            catch (Exception e)
            {
                if (this.myoutputstream!=null)
                    try { this.myoutputstream.close(); } catch (Exception e2) {}
                if (this.myinputstream!=null)
                    try { this.myinputstream.close(); } catch (Exception e2) {}
                this.myoutputstream=null;
                this.myinputstream=null;
                return("EXCEPTION: ["+e.toString()+"]");
            }
        return("");
    }

    synchronized public String unconnect()
    {
        if ((this.socket!=null)&&(this.socket.isConnected()))
            try
            {
                this.myinputstream.close();
                this.myinputstream=null;
                this.myoutputstream.close();
                this.myoutputstream=null;
                this.socket.close(); // en la prueba, 2 ms
                String res=this.create(this.legodev);
                if (res.length()>0)
                    throw new RuntimeException("Error re-creating socket: "+res);
            }
            catch (Exception e)
            {
                return("EXCEPTION: ["+e.toString()+"]");
            }
        return("");
    }

    synchronized public String sendCommand(Command c)
    // block until command is sent, or an error. Return "" if everything ok.
    {
        if (this.socket==null) return("Trying to send command to not created socket");
        if (!this.socket.isConnected()) {
            return("Trying to send command to unconnected socket");
        }
        if (this.myoutputstream==null) return("Trying to send command without output stream");

        byte[] messg=c.toBytes(true);
        try {
            this.myoutputstream.write(messg);
        }
        catch (Exception e)
        {
            return(e.toString());
        }
        return("");
    }

    synchronized public int dataAvailable()
    // return the number of bytes that have been received but not read
    {
        if ((this.myinputstream==null)||(this.socket==null)||(!this.socket.isConnected())) return(0);
        int n;
        try
        {
            n=this.myinputstream.available();
        }
        catch (Exception e) {
            return(0);
        }
        return(n);
    }

    synchronized public Response receiveResponse(long timeoutnanos)
    // block until a complete response is received, or an error occurs, or the timeout is
    // passed. Return the response or the error in the form of an error response.
    {
        long t0 = System.nanoTime();
        Response r=null;
        try
        {
            if ((this.myinputstream==null)||(this.socket==null)||(!this.socket.isConnected()))
                throw new RuntimeException("Cannot receive responses with non-functioning socket");

            int lengthdata;
            if (this.oldlen>0) lengthdata=this.oldlen;
            else {
                int numread = 0;
                byte[] bufferlen = new byte[Command.LENGTHLN];
                int offb = 0;
                while (numread < Command.LENGTHLN) {
                    int toread = Command.LENGTHLN - numread;
                    while (this.myinputstream.available() < toread)
                        if (System.nanoTime() - t0 >= timeoutnanos)
                            throw new RuntimeException("Timeout while waiting for length of response");
                    int rd = this.myinputstream.read(bufferlen, offb, toread); // up to unread length
                    if (rd < 0)
                        throw new RuntimeException("Reading length from stream reached the end!");
                    offb += rd;
                    numread += rd;
                }
                // here we have read exactly the length, and only the length, into the buffer
                lengthdata = Command.getLength(bufferlen);
                if (lengthdata<=0) throw new RuntimeException("Received message with 0 length!");
            }

            byte[] bufferbody = new byte[lengthdata];
            int offb=0;
            int numread=0;
            while (numread<lengthdata)
            {
                int toread = lengthdata-numread;
                while (this.myinputstream.available()<toread)
                    if (System.nanoTime() - t0 >= timeoutnanos) {
                        this.oldlen = lengthdata;
                        throw new RuntimeException("Timeout while waiting for length of response");
                    }
                int rd = this.myinputstream.read(bufferbody,offb,toread); // up to unread length
                if (rd<0) throw new RuntimeException("Reading body from stream reached the end!");
                offb += rd;
                numread += rd;
            }
            // here we exactly read the body
            this.oldlen=-1;

            return(Response.createFromData(bufferbody));
        }
        catch (Exception e)
        {
            try
            {
                r=new Response.Error("Exception: "+e.toString(),Command.ID.INVALID.code);
            }
            catch (Exception e2) {}
        }
        return(r);
    }

}
