package com.v2grop.lbankpulse;

/** Heuristic technical scenario weights, not calibrated event probabilities. */
public final class ScenarioEngine {
    public static final String VERSION="technical-styles-3";
    public static final class Result {
        public final int up,neutral,down,hours;
        public final double trend,momentum,rsi,efficiency,disagreement;
        public double bandPosition,breakoutSignal;
        public String regime;
        Result(int h,int u,int n,int d,double t,double m,double r,double e,double conflict){hours=h;up=u;neutral=n;down=d;trend=t;momentum=m;rsi=r;efficiency=e;disagreement=conflict;}
    }
    private static double bound(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    private static double ema(double[] c,int period){double v=c[0],a=2.0/(period+1);for(int i=1;i<c.length;i++)v+=a*(c[i]-v);return v;}
    public static Result evaluate(double[] c,int hours){
        if(c.length<80||(hours!=4&&hours!=24&&hours!=168))throw new IllegalArgumentException("Insufficient history or invalid horizon");
        if(hours==168&&c.length<336)throw new IllegalArgumentException("Weekly needs 336 closed hourly candles");
        for(double x:c)if(!Double.isFinite(x)||x<=0)throw new IllegalArgumentException("Invalid close");
        int n=c.length,look=hours==4?6:hours==24?24:168;int volWindow=hours==168?Math.min(336,c.length-1):72;double sum=0,ss=0;
        for(int i=n-volWindow;i<n;i++){double r=Math.log(c[i]/c[i-1]);sum+=r;ss+=r*r;}
        double vol=Math.sqrt(Math.max(0,(ss-sum*sum/volWindow)/(volWindow-1)));double scale=Math.max(vol,0.0005);
        double fast=ema(c,hours==4?8:hours==24?20:72),slow=ema(c,hours==4?21:hours==24?50:168);
        double trend=Math.tanh(Math.log(fast/slow)/(scale*Math.sqrt(look)));
        double momentum=Math.tanh(Math.log(c[n-1]/c[n-1-look])/(scale*Math.sqrt(look)));
        // Wilder smoothing for RSI; fixed and documented coefficients, never trained.
        double gain=0,loss=0;
        for(int i=1;i<=14;i++){double d=c[i]-c[i-1];gain+=Math.max(d,0)/14;loss+=Math.max(-d,0)/14;}
        for(int i=15;i<n;i++){double d=c[i]-c[i-1];gain=(13*gain+Math.max(d,0))/14;loss=(13*loss+Math.max(-d,0))/14;}
        double rsi=loss==0?(gain==0?50:100):100-100/(1+gain/loss);
        double strength=bound((rsi-50)/30,-1,1);
        double path=0;for(int i=n-look;i<n;i++)path+=Math.abs(c[i]-c[i-1]);
        double efficiency=path==0?0:bound(Math.abs(c[n-1]-c[n-1-look])/path,0,1);
        double conflict=(Math.abs(trend)+Math.abs(momentum)-Math.abs(trend+momentum))/2;
        double score=(hours==4?0.35:0.50)*trend+(hours==4?0.45:0.30)*momentum+0.20*strength;
        // Extreme RSI reduces directional conviction; it does not reverse a trend.
        double stretch=bound((Math.abs(rsi-50)-25)/25,0,1);
        score=bound(score*(0.5+0.5*efficiency)*(1-0.3*conflict)*(1-0.2*stretch),-1,1);
        // Bollinger-inspired position: relative location, never a standalone reversal trigger.
        int bandWindow=20;double mean=0,prevMean=0;
        for(int i=n-bandWindow;i<n;i++)mean+=c[i]/bandWindow;
        for(int i=n-bandWindow-1;i<n-1;i++)prevMean+=c[i]/bandWindow;
        double variance=0,prevVariance=0;
        for(int i=n-bandWindow;i<n;i++)variance+=(c[i]-mean)*(c[i]-mean)/bandWindow;
        for(int i=n-bandWindow-1;i<n-1;i++)prevVariance+=(c[i]-prevMean)*(c[i]-prevMean)/bandWindow;
        double sd=Math.sqrt(variance),prevSd=Math.sqrt(prevVariance);
        double z=sd>0?(c[n-1]-mean)/sd:0,prevZ=prevSd>0?(c[n-2]-prevMean)/prevSd:0;
        String regime=efficiency>=0.4?"رونددار":efficiency<=0.2?"نوسانی":"گذار";
        double bandSignal=0;
        if(efficiency>=0.4)bandSignal=Math.tanh(z/2)*efficiency;
        else if(efficiency<=0.2&&Math.abs(prevZ)>1.5&&Math.abs(z)<Math.abs(prevZ)&&Math.signum(z)==Math.signum(prevZ))
            bandSignal=-Math.signum(z)*Math.min(1,Math.abs(prevZ-z))*(1-efficiency);
        // A breakout of prior CLOSES, not an OHLC Donchian channel or institutional order flow.
        int channel=hours==4?20:hours==24?55:168;double upper=0,lower=Double.POSITIVE_INFINITY;
        for(int i=n-channel-1;i<n-1;i++){upper=Math.max(upper,c[i]);lower=Math.min(lower,c[i]);}
        double breakout=0;
        if(c[n-1]>upper)breakout=Math.tanh(Math.log(c[n-1]/upper)/scale);
        else if(c[n-1]<lower)breakout=Math.tanh(Math.log(c[n-1]/lower)/scale);
        double styleScore=0.6*breakout+0.4*bandSignal;
        double styleConflict=(Math.abs(score)+Math.abs(styleScore)-Math.abs(score+styleScore))/2;
        score=bound(0.75*score+0.25*styleScore,-1,1)*(1-0.25*styleConflict);
        conflict=bound(conflict+0.5*styleConflict,0,1);
        if(hours==168)score*=0.75;
        double neutral=bound(0.20+(hours==168?0.08:0)+0.35*(1-efficiency)+0.15*conflict-0.08*Math.abs(score),0.15,0.65);
        double[] w={(1-neutral)*(0.5+0.4*score),neutral,(1-neutral)*(0.5-0.4*score)};
        int[] rounded=round100(w);
        Result result=new Result(hours,rounded[0],rounded[1],rounded[2],trend,momentum,rsi,efficiency,conflict);
        result.bandPosition=z;result.breakoutSignal=breakout;result.regime=regime;return result;
    }
    static int[] round100(double[] w){
        int[] a=new int[3];double[] remainder=new double[3];int total=0;
        for(int i=0;i<3;i++){double v=w[i]*100;a[i]=(int)Math.floor(v);remainder[i]=v-a[i];total+=a[i];}
        while(total<100){int best=0;for(int i=1;i<3;i++)if(remainder[i]>remainder[best])best=i;a[best]++;remainder[best]=-1;total++;}
        return a;
    }
}
