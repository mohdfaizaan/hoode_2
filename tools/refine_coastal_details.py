from pathlib import Path
import re
import xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'app/src/main/res';SRC=ROOT/'app/src/main/java/com/example/hoode_app'
A='http://schemas.android.com/apk/res/android';P='http://schemas.android.com/apk/res-auto';T='http://schemas.android.com/tools'
for prefix,uri in [('android',A),('app',P),('tools',T)]:E.register_namespace(prefix,uri)
a='{'+A+'}';p='{'+P+'}'
def write(path,text):path.write_text(text,encoding='utf-8')
def save(path,tree):E.indent(tree,space='    ');tree.write(path,encoding='utf-8',xml_declaration=True)

# Retire unused duplicate activity UIs in favour of the authenticated navigation owner.
for package,name,dest in [('auth','LoginActivity','signInFragment'),('auth','RegisterActivity','signUpFragment'),('profile','EditProfileActivity','editProfileFragment'),('profile','SettingsActivity','settingsFragment')]:
    write(SRC/f'ui/{package}/{name}.kt',f'''package com.example.hoode_app.ui.{package}
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hoode_app.MainActivity
class {name} : AppCompatActivity() {{
    override fun onCreate(savedInstanceState: Bundle?) {{
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).putExtra("resident_destination", "{dest}"))
        finish()
    }}
}}
''')

# Add intentional empty states to directory flows which previously rendered a blank page.
for path,container,data,label in [
    ('providers/ProvidersFragment.kt','llProvidersContainer','list','No service providers to show'),
    ('education/EducationFragment.kt','llEducationContainer','list','No learning opportunities to show'),
    ('huffaz/HuffazFragment.kt','llHuffazContainer','list','No Huffaz profiles to show'),
    ('news/NewsFragment.kt','llNewsContainer','articles','No news to show'),
    ('events/EventsFragment.kt','llEventsContainer','eventsList','No events to show')]:
    file=SRC/'ui'/path;text=file.read_text(encoding='utf-8')
    text=text.replace('import android.os.Bundle','import com.example.hoode_app.ui.common.showEmptyContent\nimport android.os.Bundle')
    text=text.replace(f'binding.{container}.removeAllViews()',f'binding.{container}.removeAllViews()\n                if ({data}.isEmpty()) binding.{container}.showEmptyContent("{label}")')
    write(file,text)
for file in [SRC/'ui/blood/BloodNetworkFragment.kt',SRC/'ui/jobs/JobsFragment.kt']:
    text=file.read_text(encoding='utf-8').replace('R.color.border_primary','R.color.accent')
    text=text.replace('No urgent requests for $selectedGroup blood at this time.\\nAlhamdulillah, supplies are stable.','No requests to show for this blood group. Check another group or refresh the community feed.')
    write(file,text)
file=SRC/'ui/explore/CalendarFragment.kt';text=file.read_text(encoding='utf-8').replace('Color.parseColor("#171717")','holder.itemView.context.getColor(com.example.hoode_app.R.color.text_primary)');write(file,text)
file=SRC/'ui/marketplace/MarketplaceFragment.kt';text=file.read_text(encoding='utf-8').replace('Color.parseColor("#F59E0B")','requireContext().getColor(R.color.warning)').replace('Color.parseColor("#9CA3AF")','requireContext().getColor(R.color.text_secondary)');write(file,text)

# Existing shapes become aliases to the accepted cool surfaces; photography scrims remain neutral.
for file in (RES/'drawable').glob('*.xml'):
    if file.stem=='profile_cover_placeholder':
        write(file,'<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/bg_dashboard_hero"/>') if False else write(file,(RES/'drawable/bg_dashboard_hero.xml').read_text(encoding='utf-8'))
        continue
    if file.stem=='profile_placeholder':
        text=file.read_text(encoding='utf-8').replace('#E0E0E0','@color/surface_dim').replace('#BDBDBD','@color/on_navy_secondary');write(file,text)
    if not file.stem.startswith('bg_') or file.stem.startswith('bg_dashboard'):continue
    text=file.read_text(encoding='utf-8')
    mapping={'#EEF2F6':'surface_dim','#E0F2F1':'surface_dim','#EEEEEE':'surface_dim','#D9DCE1':'border_soft','#ECEDF0':'border_soft','#E7E7ED':'border_soft','#FFFFFF':'surface','#FDE8E8':'danger_soft'}
    if file.stem=='bg_progress_bar_custom':mapping['#000000']='accent'
    if file.stem.startswith('bg_carousel') or file.stem=='bg_gallery_luxury_gradient':
        for color in re.findall('#[A-Fa-f0-9]{6}',text):mapping[color]='navy'
    if file.stem=='bg_ring_circle_gold':
        mapping.update({'#F59E0B':'accent','#EC4899':'accent','#8B5CF6':'navy'})
    for old,new in mapping.items():text=text.replace('"'+old+'"','"@color/'+new+'"')
    write(file,text)

for file in (RES/'layout').glob('*.xml'):
    if 'admin' in file.stem or file.stem in ['fragment_home','item_carousel_slide','item_home_update','item_home_gallery','item_highlight_card']:continue
    tree=E.parse(file);changed=False
    for n in tree.getroot().iter():
        id=n.get(a+'id','').split('/')[-1]
        if n.get('style')=='@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox':
            n.set('style','@style/Widget.Hoode.TextInput');changed=True
        if n.get(a+'textColor')=='@color/border_primary':n.set(a+'textColor','@color/accent');changed=True
        if (id.startswith('btn_') or id.startswith('btnClose') or id.startswith('chip_')) and n.tag in ['TextView','LinearLayout','FrameLayout','ImageView']:
            n.set(a+'minHeight','48dp');n.set(a+'minWidth','48dp');n.set(a+'focusable','true');n.set(a+'clickable','true');n.set(a+'foreground','?attr/selectableItemBackground');changed=True
            if n.get(a+'layout_height','').endswith('dp') and float(n.get(a+'layout_height')[:-2])<48:n.set(a+'layout_height','48dp')
            if n.tag=='TextView':n.set(a+'gravity','center')
        if n.tag.endswith('MaterialButton') and n.get(a+'layout_height','') in ['40dp','44dp','48dp','50dp','52dp','56dp','@dimen/button_height','@dimen/button_height_primary']:
            n.set(a+'layout_height','wrap_content');n.set(a+'minHeight','48dp');changed=True
        if n.get(a+'text')=='Publish Community Photo':n.set(a+'text','Share a photo');changed=True
        if n.get(a+'text')=='Choose / Upload Photo from Gallery':n.set(a+'text','Choose a photo');changed=True
        if n.get(a+'text')=='Verified Sources Only':n.set(a+'visibility','gone');changed=True
        if n.get(a+'text','').startswith('Verified resident marketplace'):n.set(a+'text','Buy, sell and exchange with people nearby.');changed=True
        if n.tag.endswith('ScrollView') and n.get(a+'scrollbars')=='none':
            n.set(a+'scrollbars','horizontal' if n.tag=='HorizontalScrollView' else 'vertical');changed=True
    if changed:save(file,tree)

# Gallery cards must remain full bleed; global Material content padding used to inset each photo.
file=RES/'layout/item_gallery_thumbnail.xml';tree=E.parse(file);tree.getroot().set(p+'contentPadding','0dp');tree.getroot().set(p+'cardPreventCornerOverlap','false');save(file,tree)

# Drawer copy describes implemented destinations, without invented live counts or guarantees.
file=RES/'layout/layout_side_right_menu.xml';tree=E.parse(file)
copy={'Community Menu':'Your community','Live Adhan counts & 6 Masjids':'Adhan and Iqamah timetables','SOS emergency donor matching':'Donor registration and requests','24/7 Emergency Helplines':'Emergency contacts','Unlimited resident photo uploads':'Community photos and your submissions','My listings, bookmarks & security':'Your profile and submission status','Adhan alerts & announcements':'Updates on your submissions','Support & Donate ❤️':'Community contributions','Our Work 🚀':'Our projects'}
for n in tree.getroot().iter():
    if n.get(a+'text') in copy:n.set(a+'text',copy[n.get(a+'text')])
    if n.get(a+'text')=='Your community':n.set(a+'fontFamily','sans-serif-condensed');n.set(a+'textSize','28sp')
    if n.get(a+'id')=='@+id/btn_close_side_menu':n.set(a+'layout_width','48dp');n.set(a+'layout_height','48dp')
save(file,tree)

# Bottom bar grows with system text size; selected state has one consistent blue surface.
file=RES/'layout/view_compact_bottom_navigation.xml';tree=E.parse(file)
for n in tree.getroot().iter():
    id=n.get(a+'id','').split('/')[-1]
    if id=='bottomNavContainer':n.set(a+'layout_height','wrap_content');n.set(a+'minHeight','76dp');n.set(a+'elevation','2dp')
    if id in ['navHome','navExplore','navCreate','navInbox','navProfile']:
        n.set(a+'layout_height','wrap_content');n.set(a+'minHeight','66dp');n.set(a+'paddingVertical','10dp');n.set(a+'contentDescription',id[3:])
    if n.tag=='ImageView':n.set(a+'importantForAccessibility','no')
save(file,tree)
print('Resident detail screens, controls, empty states and legacy entry points refined.')
