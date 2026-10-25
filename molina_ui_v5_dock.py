#!/usr/bin/env python3
"""Molina Cinema v5: menu setting = strip ikon VERTIKAL di pojok kanan-bawah; playlist menjorok ke tepi kanan. Idempotent.
Prasyarat: v1 dan v2 sudah dijalankan."""
import os, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
MOD = os.path.join(ROOT, "feature/mpv/src/main")
RES = os.path.join(MOD, "res")
KT = os.path.join(MOD, "java/com/molina/cinema")
MA = os.path.join(KT, "MPVActivity.kt")
if not os.path.exists(os.path.join(RES, "values/molina_menu_styles.xml")) or "Widget.Molina.MenuIcon" not in open(os.path.join(RES, "values/molina_menu_styles.xml"), encoding="utf-8").read():
    sys.exit("Jalankan v1 dan v2 terlebih dahulu (root repo: %s)" % ROOT)

def rd(p): return open(p, encoding="utf-8").read()
def write(path, text):
    if os.path.exists(path) and rd(path) == text:
        print("skip  ", os.path.relpath(path, ROOT)); return
    open(path, "w", encoding="utf-8").write(text); print("tulis ", os.path.relpath(path, ROOT))

def ib(id_, desc, icon, indent="        "):
    return (f'<ImageButton\n{indent}android:id="@+id/{id_}"\n{indent}style="@style/Widget.Molina.MenuIcon"\n'
            f'{indent}android:layout_width="match_parent"\n{indent}android:layout_height="48dp"\n'
            f'{indent}android:minWidth="0dp"\n{indent}android:minHeight="48dp"\n'
            f'{indent}android:contentDescription="@string/{desc}"\n{indent}android:tooltipText="@string/{desc}"\n'
            f'{indent}android:src="@drawable/{icon}" />')
def group(id_, *items):
    inner = "\n\n        ".join(items)
    return (f'<LinearLayout\n        android:id="@+id/{id_}"\n        android:layout_width="match_parent"\n'
            f'        android:layout_height="wrap_content"\n        android:orientation="vertical">\n\n        {inner}\n\n    </LinearLayout>')

top = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Molina: menu setting = strip ikon vertikal (didok di pojok kanan-bawah oleh MPVActivity).
     ID tombol tetap. Dibungkus ScrollView agar muat di layar landscape. Tekan lama = tooltip. -->
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:fillViewport="false"
    android:overScrollMode="ifContentScrolls">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingVertical="4dp">

        <!-- Since this dialog scrolls only on small screens, don't add too many buttons here! -->

        {ib("audioBtn","open_external_audio","ic_menu_audio_file")}

        {ib("subBtn","open_external_sub","ic_menu_subtitles")}

        {ib("playlistBtn","action_playlist","ic_menu_playlist")}

        {group("rowMode",
               ib("backgroundBtn","resume_bg_playback","ic_menu_headset","            "),
               ib("audioOnlyBtn","audio_only_on","ic_menu_audiotrack","            "))}

        {group("rowChapter",
               ib("chapterBtn","chapter_button","ic_menu_chapters","            "),
               ib("chapterPrev","dialog_prev","ic_menu_prev","            "),
               ib("chapterNext","dialog_next","ic_menu_next","            "))}

        {ib("advancedBtn","advanced_menu","ic_menu_tune")}

        {ib("orientationBtn","switch_orientation","ic_menu_rotate")}

    </LinearLayout>

</ScrollView>
'''
write(os.path.join(RES, "layout/dialog_top_menu.xml"), top)

m = rd(MA); changed = False
def rep(old, new, tag, marker=None):
    global m, changed
    if (marker or new) in m: print("skip  ", tag); return
    if m.count(old) != 1: sys.exit("Pola tidak ditemukan/ganda (%s) di MPVActivity.kt" % tag)
    m = m.replace(old, new); changed = True; print("patch ", tag)

# 1) helper dockTo, sisipkan sebelum genericMenu
rep("    private fun genericMenu(",
'''    /**
     * Tempelkan dialog ke sisi layar dengan lebar tetap (dp), tanpa lebar minimum tema.
     * Gravity.END = tepi kanan, Gravity.BOTTOM = bawah; margin kecil dari tepi.
     */
    private fun AlertDialog.dockTo(gravity: Int, widthDp: Int, marginDp: Int = 8) {
        val w = window ?: return
        val dm = resources.displayMetrics
        val px = (widthDp * dm.density).toInt().coerceAtMost((dm.widthPixels * 0.92f).toInt())
        val margin = (marginDp * dm.density).toInt()
        w.setGravity(gravity)
        w.setLayout(px, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        val lp = w.attributes
        lp.x = margin
        lp.y = margin
        w.attributes = lp
    }

    private fun genericMenu(''', "helper dockTo", marker="private fun AlertDialog.dockTo(")

# 2) parameter docked di genericMenu
rep("""            restoreState: StateRestoreCallback, onViewReady: (View) -> Unit = {}) {""",
    """            restoreState: StateRestoreCallback, docked: Boolean = false,
            onViewReady: (View) -> Unit = {}) {""", "genericMenu docked param", marker="docked: Boolean = false")

# 3) dok setelah show di genericMenu (blok unik: dialog = create() ... dialog.show() }\n\n    private fun openTopMenu)
rep("""            dialog = create()
        }
        dialog.show()
    }

    private fun openTopMenu() {""",
"""            dialog = create()
        }
        dialog.show()
        if (docked) dialog.dockTo(android.view.Gravity.BOTTOM or android.view.Gravity.END, 72, 12)
    }

    private fun openTopMenu() {""", "dock top menu", marker="if (docked) dialog.dockTo(")

# 4) panggilan genericMenu untuk top menu
rep("genericMenu(R.layout.dialog_top_menu, buttons, hiddenButtons, restoreState) { v ->",
    "genericMenu(R.layout.dialog_top_menu, buttons, hiddenButtons, restoreState, docked = true) { v ->",
    "top menu docked = true")

# 5) playlist menjorok ke tepi kanan
rep("""        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
    }

    /** Ikon cari/URL di player""",
"""        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
        dialog.dockTo(android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL, 380, 8)
    }

    /** Ikon cari/URL di player""", "playlist dock kanan", marker="android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL, 380")
if changed: open(MA, "w", encoding="utf-8").write(m)

pg = os.path.join(ROOT, "progres.md")
if os.path.exists(pg):
    p = rd(pg)
    if "Cinema UI v5" not in p:
        open(pg, "w", encoding="utf-8").write(p.rstrip("\n") + """

## 2026-10-25 - Cinema UI v5 (dok menu setting + playlist)
- [x] feat(player-ui): menu setting jadi strip ikon vertikal (ScrollView) didok di pojok kanan-bawah (lebar 72dp); playlist didok ke tepi kanan (lebar maks. 380dp, tengah vertikal); helper AlertDialog.dockTo di MPVActivity
- [ ] BELUM dikompilasi (menunggu CI) dan belum diuji di perangkat (landscape dan portrait): posisi tidak tertutup bar navigasi/cutout, strip tidak terpotong, scroll saat banyak item, menu Advanced masih di tengah (belum didok)
""")
        print("patch  progres.md")
    else: print("skip   progres.md")
print("selesai.")
