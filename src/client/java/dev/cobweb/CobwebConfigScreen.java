package dev.cobweb;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public final class CobwebConfigScreen extends Screen {
 private final Screen parent;
 private TextFieldWidget delay, hex;
 private ButtonWidget bind, style;
 private boolean waiting;
 private int selectedStyle, selectedColor;
 private String status="";
 private static final int WHEEL_HEIGHT=48;

 public CobwebConfigScreen(Screen parent){super(Text.literal("Cobweb Placement Optimizer"));this.parent=parent;}
 private Text bindText(){return Text.literal("Cobweb: ").append(CobwebClient.getActivationKeyText());}
 private Text styleText(){return Text.literal("Appearance: "+new String[]{"Original","Solid color","Shimmer"}[selectedStyle]);}

 @Override protected void init(){
  int x=width/2-130,y=Math.max(8,(height-270)/2);
  selectedStyle=CobwebConfig.style;selectedColor=CobwebConfig.color;
  delay=new TextFieldWidget(textRenderer,x+130,y+34,130,20,Text.literal("Restore delay"));
  delay.setMaxLength(4);delay.setText(""+CobwebConfig.getDelayMillis());
  delay.setTextPredicate(v->v.isEmpty()||v.chars().allMatch(Character::isDigit));addDrawableChild(delay);
  bind=addDrawableChild(ButtonWidget.builder(bindText(),b->{waiting=true;b.setMessage(Text.literal("Press a key or mouse button..."));}).dimensions(x,y+62,260,20).build());
  style=addDrawableChild(ButtonWidget.builder(styleText(),b->{selectedStyle=(selectedStyle+1)%3;b.setMessage(styleText());}).dimensions(x,y+88,260,20).build());
  hex=new TextFieldWidget(textRenderer,x+130,y+116,130,20,Text.literal("Color hex"));
  hex.setMaxLength(7);hex.setText(String.format("#%06X",selectedColor));
  hex.setTextPredicate(v->v.matches("#?[0-9a-fA-F]{0,6}"));
  hex.setChangedListener(v->{if(selectedStyle==0){selectedStyle=1;style.setMessage(styleText());}});
  addDrawableChild(hex);
  addDrawableChild(ButtonWidget.builder(Text.literal("GitHub"),b->CobwebLinks.openProfile()).dimensions(x,y+218,126,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Save & Done"),b->save()).dimensions(x+134,y+218,126,20).build());
 }

 private void setBind(InputUtil.Key value){CobwebClient.setActivationKey(value);client.options.write();waiting=false;bind.setMessage(bindText());}
 @Override public boolean keyPressed(KeyInput input){
  if(waiting){if(input.key()==256){waiting=false;bind.setMessage(bindText());}else setBind(InputUtil.fromKeyCode(input));return true;}
  return super.keyPressed(input);
 }
 @Override public boolean mouseClicked(Click click,boolean doubled){
  if(waiting){setBind(InputUtil.Type.MOUSE.createFromCode(click.button()));return true;}
  int x=width/2-130,y=Math.max(8,(height-270)/2);
  if(click.x()>=x&&click.x()<x+260&&click.y()>=y+146&&click.y()<y+146+WHEEL_HEIGHT){
   float hue=(float)(click.x()-x)/260f;
   float saturation=1f-(float)(click.y()-(y+146))/WHEEL_HEIGHT;
   selectedColor=Color.HSBtoRGB(hue,saturation,1f)&0xFFFFFF;
   hex.setText(String.format("#%06X",selectedColor));
   if(selectedStyle==0){selectedStyle=1;style.setMessage(styleText());}
   return true;
  }
  return super.mouseClicked(click,doubled);
 }
 private void save(){
  try{
   int value=Integer.parseInt(delay.getText());
   String valueText=hex.getText().startsWith("#")?hex.getText().substring(1):hex.getText();
   if(value<0||value>5000||valueText.length()!=6){status="Delay: 0–5000 ms. Color: #RRGGBB.";return;}
   selectedColor=Integer.parseInt(valueText,16);
   CobwebConfig.setDelayMillis(value);CobwebConfig.color=selectedColor;CobwebConfig.style=selectedStyle;
   if(!CobwebConfig.save()){status="Could not save settings.";return;}
   if(client.world!=null)client.worldRenderer.reload();
   close();
  }catch(NumberFormatException e){status="Enter a valid delay and color.";}
 }
 @Override public void close(){client.setScreen(parent);}
 @Override public void render(DrawContext ctx,int mx,int my,float delta){
  int x=width/2-130,y=Math.max(8,(height-270)/2);
  ctx.fill(0,0,width,height,0xF008080C);ctx.fill(x-12,y-6,x+272,y+256,0xFF131019);
  ctx.fill(x-12,y-6,x+272,y-4,0xFFA855F7);
  ctx.drawCenteredTextWithShadow(textRenderer,title,width/2,y+5,0xFFC084FC);
  ctx.drawTextWithShadow(textRenderer,"Restore delay (ms)",x,y+40,0xFFE4DCEF);
  ctx.drawTextWithShadow(textRenderer,"Color (#RRGGBB)",x,y+122,0xFFE4DCEF);
  int wheelY=y+146;
  for(int col=0;col<130;col++)for(int row=0;row<12;row++){
   float hue=col/130f,saturation=1f-row/12f;
   int rgb=0xFF000000|(Color.HSBtoRGB(hue,saturation,1f)&0xFFFFFF);
   ctx.fill(x+col*2,wheelY+row*4,x+col*2+2,wheelY+row*4+4,rgb);
  }
  int preview=0xFF000000|selectedColor;
  ctx.fill(x+264,y+116,x+276,y+136,preview);
  ctx.drawCenteredTextWithShadow(textRenderer,status,width/2,y+244,0xFFFFAAAA);
  super.render(ctx,mx,my,delta);
 }
}

