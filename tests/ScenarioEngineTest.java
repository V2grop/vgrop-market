package com.v2grop.lbankpulse;
import java.util.*;
public final class ScenarioEngineTest {
 public static void main(String[] args){
  int checks=0;Random rng=new Random(18);
  for(int pattern=0;pattern<100;pattern++){
   double[] c=new double[1000];c[0]=100;
   for(int i=1;i<c.length;i++)c[i]=pattern==0?100:pattern==1?c[i-1]*1.002:pattern==2?c[i-1]/1.002:c[i-1]*Math.exp(rng.nextGaussian()*0.01);
   for(int hours:new int[]{4,24,168}){
    ScenarioEngine.Result r=ScenarioEngine.evaluate(c,hours);
    if(r.up+r.neutral+r.down!=100||Math.min(r.up,Math.min(r.neutral,r.down))<0)throw new AssertionError("Invalid weights");
    if(pattern==0&&(Math.abs(r.up-r.down)>1||r.neutral<r.up))throw new AssertionError("Flat series");
    if(pattern==1&&r.up<=r.down)throw new AssertionError("Up trend");
    if(pattern==2&&r.down<=r.up)throw new AssertionError("Down trend");
    checks++;
   }
  }
  try{ScenarioEngine.evaluate(new double[100],168);throw new AssertionError("Weekly thin history accepted");}catch(IllegalArgumentException expected){checks++;}
  double[] c=new double[336];Arrays.fill(c,100);ScenarioEngine.evaluate(c,168);checks++;
  double[] jump=new double[1000];Arrays.fill(jump,100);jump[999]=110;
  if(ScenarioEngine.evaluate(jump,4).breakoutSignal<=0)throw new AssertionError("Prior channel includes current close");checks++;
  jump[999]=90;if(ScenarioEngine.evaluate(jump,4).breakoutSignal>=0)throw new AssertionError("Down breakout");checks++;
  Arrays.fill(jump,100);ScenarioEngine.Result flat=ScenarioEngine.evaluate(jump,4);
  if(!flat.regime.equals("نوسانی")||flat.breakoutSignal!=0||flat.bandPosition!=0)throw new AssertionError("Flat regime");checks++;
  System.out.println("PASS "+checks+" scenario checks: sums, direction, flat, noise, weekly minimum");
 }
}
