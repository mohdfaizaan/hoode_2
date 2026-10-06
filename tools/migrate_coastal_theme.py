"""One-time resident resource migration. Does not touch the admin application."""
from pathlib import Path
import re
import xml.etree.ElementTree as E

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT/'app/src/main/res'
def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')
def edit(path, replacements):
    text = path.read_text(encoding='utf-8')
    for old,new in replacements: text = text.replace(old,new)
    write(path,text)

# Semantic palette is canonical. Legacy names are compatibility aliases, not separate palettes.
palette = dict(background='#F3F6FA', surface='#FFFFFF', surface_dim='#E6EDF5',
    text_primary='#172D43', text_secondary='#586A7D', accent='#235FA4',
    border_soft='#DBE3ED', navy='#183650', on_navy_secondary='#CAD9E8', amber='#F4C879',
    danger='#A13F50', danger_soft='#F8E9ED', success='#28664F', success_soft='#E5F1EB',
    warning='#825619', warning_soft='#FCF0D9', white='#FFFFFF', black='#000000',
    overlay_dark='#66172D43', overlay_light='#40FFFFFF')
aliases = {
    'surface_raised':'surface','header_gradient_start':'background','header_gradient_mid':'background','header_gradient_end':'background',
    'border_green':'border_soft','border_green_dark':'border_soft','border_green_soft':'surface_dim','border_primary':'border_soft',
    'border_faint':'background','border_subtle':'surface_dim','text_muted':'text_secondary','text_tertiary':'text_secondary',
    'text_on_accent':'white','text_on_dark':'white','text_link':'accent',
    'auth_blue':'accent','auth_purple':'navy','auth_pink':'navy','auth_background':'background','auth_card':'surface',
    'input_border':'border_soft','divider_color':'border_soft','profile_bg':'background','profile_card':'surface',
    'profile_accent_primary':'surface_dim','profile_accent_secondary':'background','profile_accent_dark':'accent',
    'profile_text_main':'text_primary','profile_text_secondary':'text_secondary','profile_divider':'border_soft',
    'profile_input_bg':'surface','profile_border':'border_soft','profile_icon_bg':'surface_dim','profile_btn_text':'accent',
    'accent_dark':'navy','accent_soft':'surface_dim','accent_ink':'text_primary','accent_surface':'surface','icon_green':'accent',
    'canva_fab_purple':'accent','canva_crown_gold':'warning','canva_crown_bg':'warning_soft','canva_badge_new':'accent','canva_pill_border':'border_soft',
    'info':'accent','info_soft':'surface_dim','emergency':'danger','emergency_soft':'danger_soft',
    'nav_active':'accent','nav_inactive':'text_secondary','nav_indicator':'surface_dim','nav_selected_red':'accent','nav_unselected':'text_secondary',
    'nav_outer_stroke':'border_soft','nav_selected_background':'surface_dim','nav_selected_stroke':'surface_dim',
    'shimmer_base':'surface_dim','shimmer_highlight':'surface','indicator_active':'accent','indicator_inactive':'border_soft',
    'prayer_ring_track':'surface_dim','prayer_ring_progress':'accent','prayer_ring_urgent':'danger','sponsored_bg':'surface_dim','sponsored_text':'text_primary',
    'status_open':'success','status_pending':'warning','status_closed':'text_secondary','status_urgent':'danger'}
for name in ['coral','orange','purple','lavender','blue','pink','teal','green','indigo','gold']:
    aliases['canva_circle_'+name] = 'accent'
old_names = {n.get('name') for n in E.parse(RES/'values/colors.xml').getroot()}
assert old_names <= set(palette) | set(aliases), old_names-set(palette)-set(aliases)
write(RES/'values/colors.xml', '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <!-- Hoode coastal palette. Compatibility aliases below keep every resident flow aligned. -->\n'+
    ''.join(f'    <color name="{k}">{v}</color>\n' for k,v in palette.items())+
    ''.join(f'    <color name="{k}">@color/{v}</color>\n' for k,v in aliases.items())+'</resources>\n')
dashboard = dict(background='background',surface='surface',ink='text_primary',secondary='text_secondary',primary='accent',soft='surface_dim',border='border_soft',navy='navy',on_navy='white',on_navy_secondary='on_navy_secondary',amber='amber',danger='danger',danger_surface='danger_soft')
p = RES/'values/dashboard.xml'; text=p.read_text(encoding='utf-8')
for k,v in dashboard.items(): text=re.sub(f'(<color name="dashboard_{k}">)[^<]+',rf'\g<1>@color/{v}',text)
write(p,text.replace('Coastal dashboard: scoped to Home and its own cards.','Home aliases preserve the approved design using the shared resident palette.'))
edit(RES/'values/dimens.xml', [('<dimen name="screen_padding_horizontal">20dp','<dimen name="screen_padding_horizontal">22dp'),('<dimen name="elevation_sm">3dp','<dimen name="elevation_sm">1dp'),('<dimen name="elevation_md">5dp','<dimen name="elevation_md">2dp'),('<dimen name="text_xs">11sp','<dimen name="text_xs">12sp'),('<dimen name="text_headline">24sp','<dimen name="text_headline">32sp'),('<dimen name="touch_target_min">44dp','<dimen name="touch_target_min">48dp')])
edit(RES/'values/styles.xml', [
    ('Hoode Connect — Canva-Inspired Component Styles','Hoode Connect — Coastal community components'),
    ('Soft elevated cards, Canva purple buttons, pill shapes','Cool surfaces, blue actions and generous spacing'),
    ('<item name="chipBackgroundColor">@color/surface</item>','<item name="chipBackgroundColor">@color/dashboard_chip_background</item>'),
    ('<item name="android:textColor">@color/text_secondary</item>\n        <item name="android:textSize">@dimen/text_sm</item>','<item name="android:textColor">@color/dashboard_chip_text</item>\n        <item name="android:textSize">@dimen/text_sm</item>'),
    ('<item name="hintTextColor">@color/text_muted</item>','<item name="hintTextColor">@color/text_secondary</item>'),
    ('<item name="cornerRadius">24dp</item>','<item name="cornerRadius">18dp</item>')])
p=RES/'values/styles.xml';text=p.read_text(encoding='utf-8')
for name in ['Display','Headline']:
    pattern = rf'(<style name="TextAppearance.Hoode.{name}".*?</style>)'
    text=re.sub(pattern,lambda m:m[0].replace('sans-serif-medium','sans-serif-condensed').replace('</style>','    <item name="android:textStyle">bold</item>\n    </style>'),text,flags=re.S)
text=text.replace('</resources>', '''    <style name="ThemeOverlay.Hoode.Dialog" parent="ThemeOverlay.Material3.MaterialAlertDialog">
        <item name="colorSurface">@color/surface</item>
        <item name="colorPrimary">@color/accent</item>
        <item name="shapeAppearanceMediumComponent">@style/ShapeAppearance.Hoode.Dialog</item>
    </style>
    <style name="ShapeAppearance.Hoode.Dialog" parent="ShapeAppearance.Material3.Corner.Large">
        <item name="cornerSize">24dp</item>
    </style>
</resources>''')
write(p,text)
edit(RES/'values/themes.xml',[('<item name="materialCardViewStyle">','<item name="android:fontFamily">sans-serif</item>\n        <item name="android:textColorPrimary">@color/text_primary</item>\n        <item name="android:textColorSecondary">@color/text_secondary</item>\n        <item name="materialAlertDialogTheme">@style/ThemeOverlay.Hoode.Dialog</item>\n        <item name="materialCardViewStyle">')])

# Normalize known neutral literals in layouts; leave photographs, brand assets and the approved Home alone.
literal = {'#222222':'text_primary','#333333':'text_primary','#444444':'text_secondary','#555555':'text_secondary',
    '#666666':'text_secondary','#888888':'text_secondary','#999999':'text_secondary','#E8E8E8':'border_soft','#EEEEEE':'border_soft',
    '#E6E6FF':'on_navy_secondary','#62E8CF':'amber','#78350F':'warning','#B45309':'warning','#F59E0B':'warning',
    '#FFFBEB':'warning_soft','#FDE8E8':'danger_soft','#1C2024':'navy'}
for p in (RES/'layout').glob('*.xml'):
    if 'admin' in p.stem or p.stem=='fragment_home': continue
    text=p.read_text(encoding='utf-8')
    for k,v in literal.items(): text=text.replace('="'+k+'"','="@color/'+v+'"')
    # Use canonical button variants even for legacy MaterialComponents callers.
    for old,new in [('Widget.MaterialComponents.Button.OutlinedButton','Widget.Hoode.Button.Secondary'),('Widget.MaterialComponents.Button.TextButton','Widget.Hoode.Button.Text')]:
        text=text.replace('@style/'+old, '@style/'+new)
    write(p,text)

# Flat light surfaces, consistent blue focus/pressed backgrounds.
for name,fill,radius,stroke in [
    ('bg_auth_gradient','navy',0,None),('bg_auth_card','surface',28,'border_soft'),
    ('bg_auth_input','surface',16,'border_soft'),('bg_edit_input','surface',16,'border_soft'),
    ('bg_input','surface',16,'border_soft'),('bg_profile_card','surface',22,'border_soft'),
    ('bg_settings_card','surface',22,'border_soft'),('bg_community_banner','surface_dim',22,None),
    ('bg_circle_light','surface_dim',100,None),('bg_primary_grey_button','surface_dim',18,None),
    ('bg_primary_button','accent',18,None),('bg_search_bar','surface',18,'border_soft')]:
    body=f'<solid android:color="@color/{fill}"/><corners android:radius="{radius}dp"/>'
    if stroke: body+=f'<stroke android:width="1dp" android:color="@color/{stroke}"/>'
    write(RES/f'drawable/{name}.xml', '<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android">'+body+'</shape>\n')
print('Resident shared palette, components and known legacy literals migrated.')
