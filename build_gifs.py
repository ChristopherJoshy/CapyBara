"""Build README GIFs with Python, Pillow and NumPy: python build_gifs.py."""

from PIL import Image,ImageDraw,ImageFont,ImageFilter
import numpy as np, math, os, random
OUT=os.path.join(os.path.dirname(os.path.abspath(__file__)),'.github','assets')
os.makedirs(OUT,exist_ok=True)
random.seed(8)
FONT_DIR=os.path.join(os.environ.get('WINDIR','C:\\Windows'),'Fonts') if os.name=='nt' else '/usr/share/fonts/truetype/dejavu'
SANS=os.path.join(FONT_DIR,'arial.ttf' if os.name=='nt' else 'DejaVuSans.ttf')
BOLD=os.path.join(FONT_DIR,'arialbd.ttf' if os.name=='nt' else 'DejaVuSans-Bold.ttf')
MONO=os.path.join(FONT_DIR,'consola.ttf' if os.name=='nt' else 'DejaVuSansMono.ttf')
C={'bg':(10,13,30),'panel':(18,27,49),'teal':(70,242,217),'pink':(255,105,163),'gold':(255,207,99),'purple':(158,120,250),'text':(246,247,254),'muted':(170,186,210),'lime':(160,250,110),'orange':(252,150,103)}
def font(z,bold=False,mono=False): return ImageFont.truetype(MONO if mono else BOLD if bold else SANS,z)
def mix(a,b,k):return tuple(int(a[j]*(1-k)+b[j]*k) for j in range(3))

def bg(w,h):
 y,x=np.indices((h,w)); arr=np.empty((h,w,3),dtype=np.uint8)
 for k in range(3):
  arr[:,:,k]=np.clip(13+(x/w)*13+(y/h)*9 + 24*np.exp(-((x-.84*w)**2/(.23*w)**2+(y-.2*h)**2/(.55*h)**2))*([0.55,0.3,1.3][k]) + 15*np.exp(-((x-.1*w)**2/(.23*w)**2+(y-.8*h)**2/(.7*h)**2))*([0.0,1.2,1.2][k]),0,255)
 return Image.fromarray(arr,'RGB')
def rounded(d, rect, rad, fill, outline=None, width=1):d.rounded_rectangle(rect,rad,fill=fill,outline=outline,width=width)
def text(d,xy,s,z=22,col=None,bold=False,anchor=None,mono=False):d.text(xy,s,font=font(z,bold,mono),fill=col or C['text'],anchor=anchor,stroke_width=0)
def dot(d,x,y,r,col):d.ellipse((x-r,y-r,x+r,y+r),fill=col)
def mascot(draw,x,y,s=1):
 # Gentle vector capybara, drawn at local scale
 p=lambda a,b:(x+a*s,y+b*s)
 draw.ellipse((*p(-112,65),*p(103,118)),fill=(18,24,40))
 draw.ellipse((*p(-90,-75),*p(90,92)), fill=(147,91,63),outline=(241,179,107),width=max(1,int(s*4)))
 draw.ellipse((*p(-34,-103),*p(-8,-52)),fill=(141,83,62),outline=(237,166,104),width=max(1,int(s*3)))
 draw.ellipse((*p(31,-102),*p(59,-55)),fill=(141,83,62),outline=(237,166,104),width=max(1,int(s*3)))
 draw.ellipse((*p(-63,-60),*p(62,65)),fill=(198,131,79))
 draw.ellipse((*p(-27,-3),*p(-13,12)),fill=(21,22,32))
 draw.ellipse((*p(27,-3),*p(41,12)),fill=(21,22,32))
 draw.ellipse((*p(-22,0),*p(-19,3)),fill=(254,248,239))
 draw.ellipse((*p(31,0),*p(34,3)),fill=(254,248,239))
 draw.ellipse((*p(-18,27),*p(34,53)), fill=(122,75,62))
 draw.ellipse((*p(0,35),*p(16,46)), fill=(34,29,35))
 draw.arc((*p(-4,39),*p(26,63)),10,165,fill=(65,44,41),width=max(1,int(2*s)))
 draw.line((*p(-79,24),*p(-113,18)),fill=(229,178,126),width=max(1,int(s*2)))
 draw.line((*p(72,22),*p(109,17)),fill=(229,178,126),width=max(1,int(s*2)))

def frame_save(frames,name,dur=85):
 # fixed adaptive palette across animation to avoid dithering flicker
 pal=frames[0].convert('P',palette=Image.Palette.ADAPTIVE,colors=128)
 data=[f.quantize(palette=pal,dither=Image.Dither.NONE) for f in frames]
 data[0].save(os.path.join(OUT,name),save_all=True,append_images=data[1:],loop=0,duration=dur,optimize=True,disposal=2)
 print(name,os.path.getsize(os.path.join(OUT,name)))

def pill(d,box,label,color,z=15):
 rounded(d,box,15,fill=(27,37,58),outline=tuple(int(v*.5) for v in color),width=2)
 text(d,((box[0]+box[2])/2,(box[1]+box[3])/2),label,z,col=color,bold=True,anchor='mm')

# HERO
w,h=1100,435
base=bg(w,h)
frames=[]
for t in range(24):
 im=base.copy(); d=ImageDraw.Draw(im)
 phase=t/24*2*math.pi
 # grid
 for xi in range(24,w,44): d.line((xi,0,xi,h),fill=(24,33,53),width=1)
 for yi in range(20,h,44): d.line((0,yi,w,yi),fill=(24,33,53),width=1)
 rounded(d,(32,27,1068,407),29,(15,22,41),outline=(50,68,89),width=2)
 dot(d,69,60,7,C['teal']);text(d,(91,49),'TEAM NEXUS  /  OPEN-SOURCE ANDROID SDK',16,C['muted'],bold=True)
 text(d,(76,110),'CAPYBARA',76,C['text'],bold=True)
 text(d,(78,211),'GIVE THE PHYSICAL WORLD A MEMORY.',24,C['teal'],bold=True)
 text(d,(80,256),'Non-invasive integrity checks for sealed packages.',19,C['muted'])
 text(d,(80,291),'Sound + vibration  /  Fully offline  /  No camera',17,C['muted'])
 pill(d,(75,341,216,380),'ON-DEVICE',C['lime'],15)
 pill(d,(227,341,365,380),'CPU FIRST',C['pink'],15)
 pill(d,(376,341,539,380),'APACHE 2.0',C['gold'],15)
 cx,cy=836,218
 for j in range(4):
  rr=int(99+40*j/3+8*math.sin(phase-j*.9));co=mix(C['teal'],C['purple'],j/4)
  d.ellipse((cx-rr,cy-rr,cx+rr,cy+rr),outline=tuple(int(v*(.8-j*.13)) for v in co),width=2)
 # package
 rounded(d,(709,303,972,357),10,(117,77,57),outline=(250,180,111),width=3)
 d.line((840,305,840,357),fill=(255,194,121),width=4)
 mascot(d,cx,cy+2,.83)
 for i in range(6):
  a=phase+i*math.pi/3; r=153+5*math.sin(phase+i)
  dot(d,cx+math.cos(a)*r,cy+math.sin(a)*r,3.8,C['pink'] if i%2 else C['teal'])
 frames.append(im)
frame_save(frames,'capybara-hero.gif',85)

# SCANNING PIPELINE
w,h=1000,285
base=bg(w,h);frames=[]
steps=[('01','ENROLL',C['teal']),('02','RE-SCAN',C['purple']),('03','ANALYZE',C['pink']),('04','DECIDE',C['lime'])]
for t in range(32):
 im=base.copy();d=ImageDraw.Draw(im)
 text(d,(37,22),'THE PHYSICAL INTEGRITY LOOP',26,C['text'],True)
 text(d,(38,61),'A trusted baseline + a repeatable probe = measurable change',15,C['muted'])
 for k,(n,title,color) in enumerate(steps):
  x=35+245*k;y=115
  active=(t//8)==k
  rounded(d,(x,y,x+214,y+116),18,(27,38,61) if active else (20,28,47),outline=color if active else (50,63,79),width=3 if active else 2)
  text(d,(x+22,y+21),n,23,color,True)
  text(d,(x+22,y+60),title,20,C['text'],True)
  notes=['Save first response','Capture another pass','Fuse signal evidence','Match / Changed / ?']
  text(d,(x+21,y+86),notes[k],13,C['muted'])
  if k<3:
   d.line((x+214,171,x+244,171),fill=(65,79,106),width=4)
   d.polygon([(x+243,165),(x+249,171),(x+243,177)],fill=color)
  if active: d.rectangle((x+13,221,x+13+int((t%8+1)/8*188),226),fill=color)
 text(d,(37,259),'Every verdict is grounded in physical evidence — not package appearance.',13,C['muted'])
 frames.append(im)
frame_save(frames,'scan-workflow.gif',100)

# SENSORS WAVEFORM
w,h=1000,305
base=bg(w,h);frames=[]
for t in range(32):
 im=base.copy();d=ImageDraw.Draw(im)
 text(d,(34,22),'MULTIMODAL PHYSICAL SENSING',26,C['text'],True)
 text(d,(35,59),'Active excitation  →  microphone + IMU  →  fused physical evidence',15,C['muted'])
 rounded(d,(35,99,965,274),22,(20,29,49),outline=(47,70,92),width=2)
 for panel,(title,co) in enumerate([('ACOUSTICS',C['teal']),('VIBRATION',C['pink']),('SENSOR FUSION',C['gold'])]):
  x=58+panel*310
  text(d,(x,114),title,17,co,True)
  for dy in [194,224]:d.line((x,dy,x+270,dy),fill=(39,54,70),width=1)
  points=[]
  for i in range(0,267,3):
   ang=(i/267*7*math.pi)+t*.26
   if panel==0: y=205+27*math.sin(ang)*(.4+.6*(.5+.5*math.sin(i*.05+t*.08)))
   elif panel==1: y=204+22*math.sin(ang*.47)*math.cos(i*.035+t*.10)
   else:y=204+19*math.sin(ang*.56)+7*math.sin(ang*1.5)
   points.append((x+i,y))
  d.line(points,fill=co,width=3,joint='curve')
 for j in range(2):d.line((346+j*310,183,362+j*310,183),fill=C['purple'],width=3)
 frames.append(im)
frame_save(frames,'sensor-fusion.gif',80)

# adaptive execution
w,h=1000,290
base=bg(w,h);frames=[]
for t in range(28):
 im=base.copy();d=ImageDraw.Draw(im)
 text(d,(32,19),'ONE SDK. DIFFERENT DEVICES.',28,C['text'],True)
 text(d,(34,58),'Adaptive modes keep the experience practical across Android hardware.',15,C['muted'])
 rounded(d,(35,93,965,270),22,(20,29,49),outline=(48,68,87),width=2)
 rounded(d,(63,116,462,243),17,(29,43,61),outline=C['teal'] if t//7%2==0 else (69,93,113),width=2)
 rounded(d,(537,116,938,243),17,(32,35,60),outline=C['purple'] if t//7%2==1 else (81,69,105),width=2)
 text(d,(91,136),'LIGHTWEIGHT PATH',20,C['teal'],True)
 text(d,(91,169),'Heuristic decision engine',16,C['text'])
 text(d,(91,198),'~85% team-reported',17,C['muted'])
 text(d,(564,136),'CAPPY-LAYA PATH',20,C['purple'],True)
 text(d,(564,169),'On-device AI decision engine',16,C['text'])
 text(d,(564,198),'Higher-resource devices',16,C['muted'])
 x=496;d.line((x,126,x,240),fill=(84,96,117),width=2)
 cy=181; rr=5+3*math.sin(t*.45);dot(d,x,cy,rr,C['gold'])
 text(d,(500,252),'CONFIGURABLE SDK POLICY',12,C['muted'],bold=True,anchor='mm')
 frames.append(im)
frame_save(frames,'adaptive-modes.gif',90)

# use cases
w,h=1000,305
base=bg(w,h);frames=[]
items=[('PARCEL DELIVERY','Dispatch → doorstep',C['teal']),('RETURNS','Detect suspicious swaps',C['pink']),('WAREHOUSING','Handover comparison',C['purple']),('EQUIPMENT','Custody and rentals',C['gold'])]
for t in range(32):
 im=base.copy();d=ImageDraw.Draw(im)
 text(d,(35,23),'BUILT FOR REAL-WORLD HANDOVERS',27,C['text'],True)
 text(d,(35,59),'When a sealed object changes hands, its physical signature matters.',15,C['muted'])
 for i,(name,desc,color) in enumerate(items):
  x=28+i*246; active=i==t//8
  rounded(d,(x,102,x+228,270),19,(33,45,65) if active else (19,29,49),outline=color if active else (47,61,80),width=3 if active else 2)
  # line art parcel
  bx=x+78;by=132
  d.polygon([(bx,by+12),(bx+31,by),(bx+67,by+13),(bx+33,by+26)],fill=(97,75,74),outline=color)
  d.polygon([(bx,by+12),(bx+33,by+26),(bx+33,by+65),(bx,by+50)],fill=(135,88,70),outline=color)
  d.polygon([(bx+33,by+26),(bx+67,by+13),(bx+67,by+52),(bx+33,by+65)],fill=(110,74,70),outline=color)
  if active:
   for j in range(2):
    r=30+j*16+(t%8)*2
    d.arc((bx+34-r,by+30-r,bx+34+r,by+30+r),210,330,fill=color,width=2)
  text(d,(x+16,218),name,15,color,True)
  text(d,(x+16,244),desc,13,C['muted'])
 frames.append(im)
frame_save(frames,'use-cases.gif',100)
