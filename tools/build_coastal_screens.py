from pathlib import Path
import xml.etree.ElementTree as E

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'app/src/main/res'
A='http://schemas.android.com/apk/res/android'; P='http://schemas.android.com/apk/res-auto'; T='http://schemas.android.com/tools'
for prefix,url in [('android',A),('app',P),('tools',T)]: E.register_namespace(prefix,url)
a='{'+A+'}'; p='{'+P+'}'
ns=f'xmlns:android="{A}" xmlns:app="{P}"'
def write(name,text): (RES/name).write_text(text,encoding='utf-8')
def save(name,tree):
    E.indent(tree,space='    ')
    tree.write(RES/name,encoding='utf-8',xml_declaration=True)
def text(label,size='14sp',color='text_secondary',extra=''):
    return f'<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="{label}" android:textSize="{size}" android:textColor="@color/{color}" {extra}/>'
def button(id,label,style='Primary',extra=''):
    return f'<com.google.android.material.button.MaterialButton android:id="@+id/{id}" style="@style/Widget.Hoode.Button.{style}" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:text="{label}" android:textAllCaps="false" {extra}/>'
def field(id,label,kind='text',autofill=''):
    password=kind=='textPassword'
    return f'''<com.google.android.material.textfield.TextInputLayout style="@style/Widget.Hoode.TextInput" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="14dp" android:hint="{label}" {'app:endIconMode="password_toggle"' if password else ''}>
        <com.google.android.material.textfield.TextInputEditText android:id="@+id/{id}" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:inputType="{kind}" android:textSize="16sp" android:autofillHints="{autofill}" android:maxLines="1" android:imeOptions="actionNext" {'android:saveEnabled="false"' if password else ''}/>
    </com.google.android.material.textfield.TextInputLayout>'''

write(Path('drawable/ic_hoode_mark.xml'),'''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="72dp" android:height="72dp" android:viewportWidth="72" android:viewportHeight="72">
    <path android:pathData="M13,32 L36,13 L59,32 M21,28 L21,49 M51,28 L51,44 M31,45 L31,32 L41,32 L41,45" android:strokeColor="@color/white" android:strokeWidth="2.8" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="@android:color/transparent"/>
    <path android:pathData="M12,53 C24,44 41,61 60,49 M12,62 C24,53 41,70 60,58" android:strokeColor="@color/on_navy_secondary" android:strokeWidth="2" android:strokeLineCap="round" android:fillColor="@android:color/transparent"/>
    <path android:pathData="M54,14 m-4,0 a4,4 0,1 0,8 0 a4,4 0,1 0,-8 0" android:fillColor="@color/amber"/>
</vector>''')
write(Path('layout/view_launch_intro.xml'),f'''<FrameLayout {ns} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/navy" android:clickable="true" android:focusable="true" android:importantForAccessibility="noHideDescendants">
    <LinearLayout android:id="@+id/introBrand" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_gravity="center" android:gravity="center" android:orientation="vertical" android:padding="32dp">
        <ImageView android:layout_width="88dp" android:layout_height="88dp" android:src="@drawable/ic_hoode_mark"/>
        {text('HOODE','48sp','white','android:gravity="center" android:layout_marginTop="18dp" android:letterSpacing="0.12" android:fontFamily="sans-serif-condensed" android:textStyle="bold"')}
        {text('A little closer to home.','16sp','on_navy_secondary','android:gravity="center" android:layout_marginTop="8dp"')}
    </LinearLayout>
    <ProgressBar android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="bottom|center_horizontal" android:layout_marginBottom="56dp" android:indeterminateTint="@color/amber" android:contentDescription="Opening Hoode"/>
</FrameLayout>''')

for signup in [False,True]:
    title='Make yourself at home.' if signup else 'Welcome home.'
    subtitle='Create your Hoode account to take part.' if signup else 'Sign in to your Hoode community.'
    form = (field('etName','Full name','textPersonName','name') if signup else '')+field('etEmail','Email address','textEmailAddress','emailAddress')+field('etPassword','Password','textPassword','newPassword' if signup else 'password')
    if signup: form+=text('Use at least 6 characters.','12sp',extra='android:layout_marginTop="6dp"')+field('etConfirmPassword','Confirm password','textPassword','newPassword')
    form+=f'<TextView android:id="@+id/tvAuthError" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:textColor="@color/danger" android:textSize="14sp" android:accessibilityLiveRegion="polite" android:visibility="gone"/>'
    form+=button('btnSignUp' if signup else 'btnSignIn','Create account' if signup else 'Sign in',extra='android:layout_marginTop="18dp"')
    if not signup: form+=button('tvForgotPassword','Need help signing in?','Text')
    form+=button('tvSignIn' if signup else 'tvGetStarted','Already a member? Sign in' if signup else 'New here? Create an account','Text')
    # Retained hidden ID until all old callers are consolidated. No pretend OAuth option.
    form+='<LinearLayout android:id="@+id/btnGoogle" android:layout_width="0dp" android:layout_height="0dp" android:visibility="gone"/>'
    content=f'''<androidx.core.widget.NestedScrollView {ns} android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true" android:background="@color/background" android:clipToPadding="false">
      <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingBottom="28dp">
       <LinearLayout android:id="@+id/topHeader" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@color/navy" android:orientation="vertical" android:padding="22dp" android:paddingBottom="54dp">
        <LinearLayout android:layout_width="match_parent" android:layout_height="48dp" android:gravity="center_vertical">
         <ImageButton android:id="@+id/btnBack" android:layout_width="48dp" android:layout_height="48dp" android:background="?attr/selectableItemBackgroundBorderless" android:src="@drawable/ic_arrow_back" android:padding="12dp" app:tint="@color/white" android:contentDescription="Back"/>
         <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="end" android:text="HOODE" android:textColor="@color/white" android:fontFamily="sans-serif-condensed" android:textStyle="bold" android:textSize="22sp" android:letterSpacing="0.08"/>
        </LinearLayout>
        <ImageView android:layout_width="62dp" android:layout_height="62dp" android:layout_marginTop="14dp" android:src="@drawable/ic_hoode_mark" android:importantForAccessibility="no"/>
        {text(title,'38sp','white','android:fontFamily="sans-serif-condensed" android:textStyle="bold" android:layout_marginTop="12dp"')}
        {text(subtitle,'15sp','on_navy_secondary','android:layout_marginTop="8dp"')}
       </LinearLayout>
       <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginHorizontal="22dp" android:layout_marginTop="-26dp" android:background="@drawable/bg_auth_card" android:elevation="2dp" android:padding="22dp" android:orientation="vertical">
        {text('Join the community' if signup else 'Your account','23sp','text_primary','android:fontFamily="sans-serif-medium"')}
        {form}
       </LinearLayout>
      </LinearLayout>
    </androidx.core.widget.NestedScrollView>'''
    write(Path('layout/fragment_signup.xml' if signup else 'layout/fragment_signin.xml'),content)

# Explore is a readable service directory, rather than two-column cards with truncated names.
tree=E.parse(RES/'layout/fragment_explore.xml'); root=tree.getroot()
root.insert(0,E.fromstring(text('Explore Hoode.','36sp','text_primary',f'xmlns:android="{A}" android:paddingHorizontal="22dp" android:paddingTop="22dp" android:fontFamily="sans-serif-condensed" android:textStyle="bold"')))
for n in root.iter():
    if n.get(a+'id')=='@+id/et_explore_search': n.set(a+'hint','Search services, places and more')
    if n.get(a+'id')=='@+id/btn_clear_search':
        for k,v in {'layout_width':'48dp','layout_height':'48dp','padding':'14dp','contentDescription':'Clear search','focusable':'true','background':'?attr/selectableItemBackgroundBorderless'}.items(): n.set(a+k,v)
    if n.get(a+'id','').startswith('@+id/row_'):
        n.set(a+'orientation','vertical')
        for child in list(n):
            if child.tag=='View': n.remove(child);continue
            child.set(a+'layout_width','match_parent')
            for k in ['layout_weight','layout_marginStart','layout_marginEnd']: child.attrib.pop(a+k,None)
    if n.get(a+'id')=='@+id/ll_explore_categories':
        n.insert(0,E.fromstring(text('No matches. Try a service or place name.','15sp','text_secondary',f'xmlns:android="{A}" android:id="@+id/tvSearchEmpty" android:padding="22dp" android:visibility="gone" android:accessibilityLiveRegion="polite"')))
save(Path('layout/fragment_explore.xml'),tree)
tree=E.parse(RES/'layout/item_explore_feature.xml');root=tree.getroot()
for k,v in {'cardCornerRadius':'20dp','contentPadding':'16dp','cardElevation':'0dp','cardPreventCornerOverlap':'false'}.items():root.set(p+k,v)
for k,v in {'layout_marginStart':'0dp','layout_marginEnd':'0dp','clickable':'true','focusable':'true'}.items():root.set(a+k,v)
for n in root.iter():
    if n.get(a+'id')=='@+id/iv_feature_icon':n.set(p+'tint','@color/accent')
    if n.get(a+'id')=='@+id/tv_feature_desc':
        n.set(a+'textSize','13sp');n.attrib.pop(a+'maxLines',None);n.attrib.pop(a+'ellipsize',None)
save(Path('layout/item_explore_feature.xml'),tree)

# Shared detail-header typography, touch targets and scalable cards.
for file in (RES/'layout').glob('*.xml'):
    if 'admin' in file.stem or file.stem in ['fragment_home','item_carousel_slide','item_highlight_card','item_home_update','item_home_gallery','fragment_signin','fragment_signup']:continue
    tree=E.parse(file); root=tree.getroot(); changed=False
    for parent in root.iter():
        children=list(parent)
        back=next((n for n in children if n.get(a+'id') in ['@+id/btn_back','@+id/btnBack']),None)
        if back is not None:
            back.set(a+'layout_width','48dp');back.set(a+'layout_height','48dp');back.set(a+'padding','12dp');back.set(a+'focusable','true');changed=True
            for n in children:
                if n.tag=='TextView' and n.get(a+'text') and n.get(a+'layout_weight')=='1':
                    n.set(a+'fontFamily','sans-serif-condensed');n.set(a+'textStyle','bold');n.set(a+'textSize','28sp')
        if parent.tag=='com.google.android.material.card.MaterialCardView':
            # An explicitly padded child must not inherit a second 16dp inner margin.
            if any(n.get(a+'padding') or n.get(a+'paddingHorizontal') for n in children) and p+'contentPadding' not in parent.attrib:
                parent.set(p+'contentPadding','0dp');parent.set(p+'cardPreventCornerOverlap','false');changed=True
        if parent.get(a+'id','').split('/')[-1] in ['btnCompleteProfile','btnEditProfile']:
            parent.set(a+'layout_height','wrap_content');parent.set(a+'minHeight','48dp');parent.set(a+'paddingVertical','12dp');parent.set(a+'focusable','true');parent.set(a+'foreground','?attr/selectableItemBackground');changed=True
    if changed: save(file.relative_to(RES),tree)

# Profile: put editing and the submission tabs before private details, keeping all binding IDs.
tree=E.parse(RES/'layout/fragment_profile.xml');root=tree.getroot()
content=next(n for n in root.iter() if any(ch.get(a+'id')=='@+id/cardProfileAvatar' for ch in n))
details=next(n for n in content if n.get(a+'id')=='@+id/cardResidentDetails')
content.remove(details);content.append(details)
completion=next(n for n in content if n.get(a+'id')=='@+id/cardProfileCompletion')
actions=next(n for n in content if any(ch.get(a+'id')=='@+id/btnEditProfile' for ch in n))
content.remove(actions);content.insert(list(content).index(completion),actions)
content.set(a+'paddingStart','22dp');content.set(a+'paddingEnd','22dp')
placeholders={'tvName':'Your profile','tvUsername':'','tvStatConnections':'0','tvStatContributions':'0','tvDetailEmail':'Not provided','tvDetailStatus':'Resident','tvCompletionPercentage':'','tvCompletionTip':'Complete your profile to help the community get to know you.'}
for n in root.iter():
    id=n.get(a+'id','').split('/')[-1]
    if id in placeholders:n.set(a+'text',placeholders[id])
    if id=='tvName':n.set(a+'fontFamily','sans-serif-condensed');n.set(a+'textSize','32sp')
    if id=='btnEditProfile':n.set(a+'text','Edit profile')
    if id=='profileTabs':n.set(p+'tabMode','scrollable');n.set(p+'tabMinWidth','100dp');n.set(p+'tabRippleColor','@color/surface_dim');n.set(p+'tabSelectedTextColor','@color/accent')
    if id=='statsCard':
        n.set(a+'layout_height','wrap_content');n.set(a+'minHeight','88dp');n.set(a+'paddingVertical','16dp')
        for ch in n:
            if ch.tag=='LinearLayout':ch.set(a+'layout_height','wrap_content')
    if id=='pbProfileCompletion':n.set(a+'progress','0')
    if n.tag.endswith('ScrollView'):n.set(a+'scrollbars','vertical')
save(Path('layout/fragment_profile.xml'),tree)

# Generous create rows and a clear review explanation.
tree=E.parse(RES/'layout/fragment_create.xml');root=tree.getroot(); content=list(root)[0]
for n in root.iter():
    if n.tag=='com.google.android.material.card.MaterialCardView':
        n.set(a+'layout_marginStart','0dp');n.set(a+'layout_marginEnd','0dp');n.set(p+'cardElevation','0dp');n.set(a+'clickable','true');n.set(a+'focusable','true')
    if n.get(a+'text')=='Create & Share':n.set(a+'text','Share with Hoode.')
    if n.get(a+'text')=='Resident Hub':n.set(a+'visibility','gone')
    if n.get(a+'text','').startswith('Select a category'):n.set(a+'text','Have something to share? Choose where it belongs. You can follow every submission in Profile → Activity.')
save(Path('layout/fragment_create.xml'),tree)

tree=E.parse(RES/'layout/item_settings_row.xml');root=tree.getroot()
root.set(a+'layout_height','wrap_content');root.set(a+'minHeight','64dp');root.set(a+'paddingVertical','14dp');root.set(a+'background','?attr/selectableItemBackground');root.set(a+'focusable','true')
for n in root:
    if n.get(a+'id')=='@+id/tvValue':n.set(a+'maxWidth','104dp');n.set(a+'gravity','end')
save(Path('layout/item_settings_row.xml'),tree)
for name in ['fragment_settings','activity_settings']:
    tree=E.parse(RES/f'layout/{name}.xml')
    for n in tree.getroot().iter():
        if n.get(a+'text')=='Mohammed Suhail':n.set(a+'text','Your account')
        if n.get(a+'text')=='mohammed@email.com':n.set(a+'text','')
        if n.get(a+'layout_height') in ['76dp','92dp']:
            n.set(a+'minHeight',n.get(a+'layout_height'));n.set(a+'layout_height','wrap_content');n.set(a+'paddingVertical','16dp')
    save(Path(f'layout/{name}.xml'),tree)
print('Auth, launch, Explore, profile, create and shared content layouts updated.')
