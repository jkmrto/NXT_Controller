package admtr.practica7;/*
    JAVA class with the definitions of the messages for the Lego NXT Direct Commands
    (c) Juan Antonio Fernández Madrigal, 2015
*/

// Main public class: a command that can be issued to the lego NXT
// The transmission frame contains a length (2 bytes) plus the data of the response itself.
public abstract class Command
{
    // ------- Public constants, types, etc.

    // ID of the commands that can be sent to the Lego NXT
    public enum ID
    {
        INVALID((byte)0x00),            // invalid ID
        GETFIRMVERSION((byte)0x88),     // no arguments
        GETDEVICEINFO((byte)0x9B),      // no arguments
        PLAYTONE((byte)0x03),           // two arguments: freq in Hz, dur in ms
        GETBATTERYLEVEL((byte)0x0B),    // no arguments
        SETOUTPUTSTATE((byte)0x04),     // three arguments: output port (0..2),
                                        // power (-100..100),
                                        // off (0 or 1; if 1 the second argument
                                        // is ignored)
        GETOUTPUTSTATE((byte)0x06),     // one argument: output port (0..2)
        RESETMOTORPOSITION((byte)0x0A); // one argument: output port (0..2)

        ID(byte c)
        {
            this.code=c;
        }

        public final byte code; // code corresponding to the ID
    };

    // ------- Public class attributes

    static public final byte LENGTHLN=2; // bytes occupied by the length field
                                         // in the data frame of responses and
                                         // commands

    // ------- Public class methods

    // return LSB+MSB bytes of an int
    static public byte[] getLMSBfromInt(int v) throws IllegalArgumentException
    {
        if ((v>65535)||(v<=0))
            throw new IllegalArgumentException("Invalid word");
        byte[] result=new byte[2];
        result[0]= (byte)(v & 0xff);
        result[1]= (byte)((v & 0xff00)>>8);
        return(result);
    }

    // return the int corresponding to LSB+MSB bytes
    static public int getIntFromLMSB(byte lsb, byte msb)
    {
        int l=(lsb<0?256+(int)lsb:lsb);
        int m=(msb<0?256+(int)msb:msb);
        return(l+(m<<8));
    }

    // return the length contained in the data frame of a response or command
    static public int getLength(byte[] data) throws IllegalArgumentException
    {
        if (data.length<LENGTHLN)
            throw new IllegalArgumentException("Data frame with less than 2 bytes");
        return(getIntFromLMSB(data[0],data[1]));
    }

    // Return the message body discarding the embedded length, both for
    // commands and responses
    static public byte[] getBody(byte[] msg) throws IllegalArgumentException
    {
        if (msg.length<LENGTHLN+1)
            throw new IllegalArgumentException("Message with <3 bytes");
        int leninmsg=getLength(msg);
        byte[] bdata=new byte[leninmsg];
        System.arraycopy(msg,2,bdata,0,leninmsg);
        return(bdata);
    }

    // ------- Common methods of all derived commands

    // abstract method that must return the ID of the derived command
    public abstract ID myID();

    // abstract method that must have every derived command to form the command
    // in byte array form
    public abstract byte[] toBytes(boolean withresponse);



    // ------- Inner classes that inherits from Command for specific commands

    public static class GetFirmVersion extends Command
    // non-static (i.e., inner) classes can only
    // exist in the context of an object of the
    // enclosing class; static ones can exist
    // standalone.
    // Here we only want to use the enclosing
    // class as a namespace for code clarity and
    // for providing common functionality to
    // these derived classes (they cannot be
    // declared in this same file outside the
    // scope of Command; they would require
    // separate files)
    // See:
    // https://docs.oracle.com/javase/tutorial/
    //         java/javaOO/nested.html
    {
        public GetFirmVersion()
        {
        }

        public byte[] toBytes(boolean withresponse)
        {
            byte[] comm = new byte[]{0x01, ID.GETFIRMVERSION.code};
            return(addLengthAndResponse(comm, !withresponse));
        }

        public ID myID()
        {
            return(ID.GETFIRMVERSION);
        }

    };

    public static class GetDeviceInfo extends Command
    {
        public GetDeviceInfo()
        {
        }

        public byte[] toBytes(boolean withresponse)
        {
            byte[] comm = new byte[]{0x01, ID.GETDEVICEINFO.code};
            return(addLengthAndResponse(comm, !withresponse));
        }

        public ID myID()
        {
            return(ID.GETDEVICEINFO);
        }

    };

    public static class PlayTone extends Command
    {
        private int fr,du;

        // Constructor: requires frequency (in [200,14000]Hz) and duration (ms)
        public PlayTone(int freq, int dur) throws IllegalArgumentException
        {
            if ((freq<=200)||(freq>=14000))
                throw new IllegalArgumentException("Invalid frequency");
            if (dur<=0)
                throw new IllegalArgumentException("Invalid duration");
            fr=freq;
            du=dur;
        }

        public int getfrequency()
        {
            return(fr);
        }

        public int getduration()
        {
            return(du);
        }

        public byte[] toBytes(boolean withresponse)
        {
            byte[] f=getLMSBfromInt(fr);
            byte[] d=getLMSBfromInt(du);
            byte[] comm=new byte[]{0x00, ID.PLAYTONE.code,
                                   f[0], f[1], d[0], d[1]};
            return(addLengthAndResponse(comm, !withresponse));
        }

        public ID myID()
        {
            return(ID.PLAYTONE);
        }

    };

    public static class GetBatteryLevel extends Command
    {
        public GetBatteryLevel()
        {
        }

        public byte[] toBytes(boolean withresponse)
        {
            byte[] comm = new byte[]{0x00, ID.GETBATTERYLEVEL.code};
            return(addLengthAndResponse(comm, !withresponse));
        }

        public ID myID()
        {
            return(ID.GETBATTERYLEVEL);
        }

    };

    public static class SetOutputState extends Command
    {
        private byte port;
        private boolean off;
        private byte power;

        // constructor for an OFF command
        public SetOutputState(byte p) throws IllegalArgumentException
        {
            if ((p<0)||(p>2))
                throw new IllegalArgumentException("Invalid output port");
            port=p;
            off=true;
            power=0;
        }

        // constructor for an ON command
        public SetOutputState(byte p, byte po) throws IllegalArgumentException
        {
            if ((p<0)||(p>2))
                throw new IllegalArgumentException("Invalid output port");
            if ((po < -100) || (po > 100))
                throw new IllegalArgumentException("Invalid power");
            port=p;
            off=false;
            power=po;
        }

        public byte getport()
        {
            return(port);
        }

        public boolean getoff()
        {
            return(off);
        }

        public byte getpower()
        {
            return(power);
        }

        public byte[] toBytes(boolean withresponse)
        {
            byte mode,runstate;
            if (off)
            {
                mode=0x02;
                runstate=0x00;
            }
            else
            {
                mode=0x01;
                runstate=0x20;
            }
            byte [] comm=new byte[]{ 0x00, ID.SETOUTPUTSTATE.code,
                                     port, power, mode,
                                     0x00, 0x00, runstate,
                                     0x00, 0x00, 0x00, 0x00 };
            return(addLengthAndResponse(comm, !withresponse));
        }

        public ID myID()
        {
            return(ID.SETOUTPUTSTATE);
        }

    }

    public static class GetOutputState extends Command
    {
        private byte port;

        public GetOutputState(byte p) throws IllegalArgumentException {
            if ((p < 0) || (p > 2))
                throw new IllegalArgumentException("Invalid output port");
            port = p;
        }

        public byte getport() {
            return (port);
        }

        public byte[] toBytes(boolean withresponse) {
            byte[] comm = new byte[]{0x00, ID.GETOUTPUTSTATE.code, port};
            return (addLengthAndResponse(comm, !withresponse));
        }

        public ID myID()
        {
            return(ID.GETOUTPUTSTATE);
        }

    };

    public static class ResetMotorPosition extends Command
    {
        private byte port;

        public ResetMotorPosition(byte p) throws IllegalArgumentException {
            if ((p < 0) || (p > 2))
                throw new IllegalArgumentException("Invalid output port");
            port = p;
        }

        public byte getport() {
            return (port);
        }

        public byte[] toBytes(boolean withresponse)
        {
            // absolute reset
            byte[] comm=new byte[]{ 0x00, ID.RESETMOTORPOSITION.code,
                                    port, 0};
            return (addLengthAndResponse(comm, !withresponse));
        }

        public ID myID()
        {
            return(ID.RESETMOTORPOSITION);
        }

    };


    // --------------------- Protected methods of the Command base class

    // preffix the bytes with their length
    static protected byte[] addLengthAndResponse(byte[] c,
                                                 boolean withoutresponse)
            throws IllegalArgumentException
    {
        int l=c.length;
        if (l<=0) throw new IllegalArgumentException("Null message length");
        // max. length of a message must be 64+2 bytes, according to the NXT
        byte[] len=getLMSBfromInt(l);
        byte[] result=new byte[l+2];
        System.arraycopy(len,0,result,0,2);
        System.arraycopy(c,0,result,2,l);
        if (withoutresponse) result[0] |= (byte) 0x80;
        return(result);
    }

};



