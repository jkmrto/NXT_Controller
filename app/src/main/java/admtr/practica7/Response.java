package admtr.practica7;/*
    JAVA class with the definitions of the messages of response for the Lego NXT
    Direct Commands
    (c) Juan Antonio Fernández Madrigal, 2015

    USAGE: Create one of the classes derived from Response (see below) from the
    data received from the Lego; if that class is not the correct one, an
    InvalidClassException will be raised; if the data is invalid, an
    IllegalArgumentException will be raised. You can begin by trying with the
    Error response for detecting error before success. Each derived response has
    also a method to tell if the block of data received corresponds to it,
    before trying to construct.
*/


import java.io.InvalidClassException;
import java.util.IllegalFormatCodePointException;

// Main public class: a response that can be received after a direct Lego command
// The transmission frame contains a length (2 bytes) plus the data of the response itself.
// (See the Command class for more info)
public abstract class Response
{

    // ------- Common methods of all derived responses

    // abstract method that must return the ID of the command corresponding to
    // the derived response
    public abstract Command.ID myCommandID();

    // abstract method that must return the length of the response block of the
    // derived response, without counting the embedded length
    public abstract byte respLength();

    // Check whether the data corresponds to a response or to the error
    // (if INVALID); DATA cannot have the embedded length
    public static boolean isIt(byte[] data, Command.ID id)
            throws IllegalArgumentException
    {
        if (data.length<3)
            throw new IllegalArgumentException("Responses must be >= 3 bytes");
        if (id==Command.ID.INVALID)
            return(data[2]!=0x00); // not checking the ID, but the status
        else return(data[1]==id.code);
    }

    static public Response createFromData(byte[] responsedata) throws IllegalArgumentException
    // return a response of the correct type constructed from the data, which does not
    // contain the length field, only the body
    {
        try {
            if (responsedata[1] == Command.ID.GETFIRMVERSION.code)
                return (new Response.GetFirmVersion(responsedata));
            if (responsedata[1] == Command.ID.GETFIRMVERSION.code )
                return (new Response.GetFirmVersion(responsedata));
            if (responsedata[1] == Command.ID.GETDEVICEINFO.code )
                return (new Response.GetDeviceInfo(responsedata));
            if (responsedata[1] == Command.ID.PLAYTONE.code )
                return (new Response.PlayTone(responsedata));
            if (responsedata[1] == Command.ID.GETBATTERYLEVEL.code )
                return (new Response.GetBatteryLevel(responsedata));
            if (responsedata[1] == Command.ID.SETOUTPUTSTATE.code )
                return (new Response.SetOutputState(responsedata));
            if (responsedata[1] == Command.ID.GETOUTPUTSTATE.code )
                return (new Response.GetOutputState(responsedata));
            if (responsedata[1] == Command.ID.RESETMOTORPOSITION.code )
                return (new Response.ResetMotorPosition(responsedata));
        }
        catch (Exception e)
        {
            return(new Response.Error("Exception: "+e.toString(),(byte)0));
        }
        try {
            return(new Response.Error(responsedata));
        } catch (Exception e)
        {
            return(new Response.Error("Exception forming error response: "+e.toString(),(byte)0));
        }
    }


    // ------- Derived classes for the particular responses, including the error
    // See first note in Command.java
    // All DATA in constructors are without the embedded length

    public static class Error extends Response
    {
        public final byte command;  // command that produced the error
        public final byte errorcode;
        public final String errortxt;

        public Error(String s, byte co)
        // construct an ad-hoc error response for that text and command
        {
            this.command=co;
            this.errorcode=0;
            this.errortxt=s;
        }

        public Error(byte[] data)
                throws IllegalArgumentException, InvalidClassException
        {
            if (data.length<3)
                throw new IllegalArgumentException("Invalid data length");
            if (data[2]==0x00)
                throw new InvalidClassException("Not an error response");
            command=data[1];
            errorcode=data[2];
            errortxt=respErrorText(data[2]);
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.INVALID); // no especific command produces errors
        }

        public byte respLength()
        {
            return(3);
        }

    };

    public static class GetFirmVersion extends Response
    {
        public final byte minor_proto;
        public final byte major_proto;
        public final byte minor_firm;
        public final byte major_firm;

        public GetFirmVersion(byte[] data) throws IllegalArgumentException,
                                                  InvalidClassException
        {
            // response: 0x02 0x88 status
            //           minor_prot major_prot
            //           minor_firm major_firm
            checkrespdata(data);
            minor_proto=data[3];
            major_proto=data[4];
            minor_firm=data[5];
            major_firm=data[6];
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.GETFIRMVERSION);
        }

        public byte respLength()
        {
            return(7);
        }
    };

    public static class GetDeviceInfo extends Response
    {
        public final String NXTname;
        public final byte[] bluetoothMAC;
        public final int freeflash;

        public GetDeviceInfo(byte[] data) throws
                                                IllegalFormatCodePointException,
                                                InvalidClassException
        {
            // resp: 0x02, 0x9B, status,
            //       nxt name and null end (15 bytes),
            //       MAC (7 bytes),
            //       bluetooth strength, unimplemented (LSB),
            //       bluetooth strength, unimplemented (2nd byte)
            //       bluetooth strength, unimplemented (3rd byte)
            //       bluetooth strength, unimplemented (MSB),
            //       free user flash in bytes (LSB),
            //       free user flash in bytes (2nd byte),
            //       free user flash in bytes (3rd byte),
            //       free user flash (MSB)
            checkrespdata(data);
            byte[] aux=new byte[15];
            System.arraycopy(data,3,aux,0,15);
            NXTname=bytearraytostring(aux);
            aux=new byte[7];
            System.arraycopy(data,18,aux,0,7);
            bluetoothMAC=aux.clone();
            freeflash=(int)bytearraytolong(new byte[]{data[29], data[30],
                                                      data[31], data[32]});
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.GETDEVICEINFO);
        }

        public byte respLength()
        {
            return(33);
        }
    };

    public static class PlayTone extends Response
    {
        public PlayTone(byte[] data) throws
                                            IllegalFormatCodePointException,
                                            InvalidClassException
        {
            // resp: 0x02, 0x03, status_byte
            checkrespdata(data);
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.PLAYTONE);
        }

        public byte respLength()
        {
            return(3);
        }
    };

    public static class GetBatteryLevel extends Response
    {
        public final int millivolts;

        public GetBatteryLevel(byte[] data) throws
                                                IllegalFormatCodePointException,
                                                InvalidClassException
        {
            // resp: 0x02, 0x0b, status,
            // millivolts_LSB, millivolts_MSB
            checkrespdata(data);
            millivolts=Command.getIntFromLMSB(data[3],data[4]);
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.GETBATTERYLEVEL);
        }

        public byte respLength()
        {
            return(5);
        }
    };

    public static class SetOutputState extends Response
    {
        public SetOutputState(byte[] data) throws
                IllegalFormatCodePointException,
                InvalidClassException
        {
            // resp: 0x02, 0x04, status
            checkrespdata(data);
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.SETOUTPUTSTATE);
        }

        public byte respLength()
        {
            return(3);
        }
    };

    public static class GetOutputState extends Response
    {
        public final byte port;
        public final byte power;
        public final boolean off;
        public final int tacho;

        public GetOutputState(byte[] data) throws
                IllegalFormatCodePointException,
                InvalidClassException
        {
            // resp:
            //  +0: 0x02,
            //  +1: 0x06,
            //  +2: status,
            //  +3: port,
            //  +4: power,
            //  +5: mode (0x00 -> motor off, 0x01 -> motor on, 0x02 -> break, 0x04 -> regulated),
            //  +6: reg.mode (0x00 -> no reg., 0x01 -> speed, 0x02 -> sync),
            //  +7: turn ratio,
            //  +8: run state (0x00 -> idle, 0x10 -> ramp up, 0x20 -> running, 0x40 -> ramp down),
            //  +9..+12: tacho limit (4 bytes unsigned, little endian; 0 if no tacho limit),
            //  +13..+16: tacho count (4 bytes signed, little endian, degrees since last reset of motor counter),
            //  +17..+20: block tacho count (4 bytes signed, little endian, position w.r.t. last programmed motor movement, degrees),
            //  +21..+24: rotation count (4 bytes, signed, little endian, since last reset of rotation sensor, degrees).
            checkrespdata(data);
            port=data[3]; // port
            if ( (data[4]==0) && // power
                  ((data[5]==0x00)||(data[5]==0x02)) && // reg.mode
                  (data[8]==0x00) ) // run state
            {
                power=0;
                off=true;
            }
            else
            {
                power=data[4];
                off=false;
            }
            tacho=(int)bytearraytolong(new byte[]{data[13], data[14],
                                                  data[15], data[16]});
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.GETOUTPUTSTATE);
        }

        public byte respLength()
        {
            return(25);
        }
    };

    public static class ResetMotorPosition extends Response
    {
        public ResetMotorPosition(byte[] data) throws
                IllegalFormatCodePointException,
                InvalidClassException
        {
            // resp: 0x00, 0x0A, status
            checkrespdata(data);
        }

        public Command.ID myCommandID()
        {
            return(Command.ID.RESETMOTORPOSITION);
        }

        public byte respLength()
        {
            return(3);
        }
    };



    // ------------------- Private/protected methods of the Command base class

    // Check whether the response block of data is correct (without the
    // embedded length)
    protected void checkrespdata(byte[] data) throws IllegalArgumentException,
                                                     InvalidClassException
    {
        if (data.length!=this.respLength())
            throw new IllegalArgumentException("Invalid data length");
        if (data[0]!=0x02)
            throw new IllegalArgumentException("Not a response");
        if (data[1]!=this.myCommandID().code)
            throw new InvalidClassException("Not the correct response");
        if (data[2]!=0x00)
            throw new InvalidClassException("Error response");
    }

    // Convert zero-ended byte array to string
    protected static String bytearraytostring(byte[] data)
    {
        String res="";
        for (byte b: data)
            if (b==0x00) break;
            else res+=(char)b;
        return(res);
    }

    // Convert little-endian byte array to long
    protected static long bytearraytolong(byte[] data)
    {
        long res=0;
        for (int f=data.length-1; f>=0; f--)
            res = res*(long)256+( (long)(data[f]) & 0xff ); // mask needed to discard sign of data[f]
        return(res);
    }

    // Return a description of the error contained in the response
    protected static String respErrorText(byte e)
    {
        switch ((int)e) {
            case 0x20:
                return ("Pending communication transaction in progress");
            case 0x40:
                return ("Specified mailbox queue is empty");
            case 0xBD:
                return ("Request failed (i.e. specified file not found)");
            case 0xBE:
                return ("Unknown command opcode");
            case 0xBF:
                return ("Insane packet");
            case 0xC0:
                return ("Data contains out-of-range values");
            case 0xDD:
                return ("Communication bus error");
            case 0xDE:
                return ("No free memory in communication buffer");
            case 0xDF:
                return ("Specified channel/connection is not valid");
            case 0xE0:
                return ("Specified channel/connection not configured or busy");
            case 0xEC:
                return ("No active program");
            case 0xED:
                return ("Illegal size specified");
            case 0xEE:
                return ("Illegal mailbox queue ID specified");
            case 0xEF:
                return ("Attempted to access invalid field of a structure");
            case 0xF0:
                return ("Bad input or output specified");
            case 0xFB:
                return ("Insufficient memory available");
            case 0xFF:
                return ("Bad arguments");
        }
        return("Unknown error code");
    }

};