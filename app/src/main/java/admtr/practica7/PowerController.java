package admtr.practica7;

/**
 * Created by root on 15/01/17.
 */

public class PowerController {

    public final static double MAXABSV = 0.4; // max linear speed of Lego
    public final static double MAXABSW = 7; // max angular speed of Lego
    public final static double ABSINCV = MAXABSV / (5.0);
    public final static double ABSINCW = MAXABSW / 40.0;
    private final double d = 0.115; // distance between wheels
    private final double r = 0.03; // wheel radius
    public double v=0;
    public double w=0;


    byte powerForWheel(boolean leftorright)
// return the power to give to the wheel motor in order to get those velocities
// only works well if the robot has its battery full!
    {
        double vwheel; // m/s  -> linear speed of the center of the wheel on the plane
        if (leftorright) // left wheel
            vwheel = v - d * w;
        else // right wheel
            vwheel = v + d * w;

        double alpha = vwheel / r; // -> rad/s -> angular speed of rotation for the wheel

        double power;
        if (Math.abs(alpha)<1e-6) power=0.0;
        else power = (alpha - 0.031571)/0.14149;

        if (power<-100.0) return(-100);
        else if (power>100.0) return(100);

        return((byte)Math.round(power));
    }


    void parar(){
        this.v = 0;
        this.w = 0;
    }

    void incrementar_V(){

        this.v = v + ABSINCV;
    }

    void decrementar_V(){
        this.v = v - ABSINCV;
    }

    void incrementar_W(){

        this.w = w + ABSINCW;;
    }

    void decrementar_W(){

        this.w = w - ABSINCW;
    }

    void straight(){

        this.w = 0;
        this.w = 0;
    }


}
