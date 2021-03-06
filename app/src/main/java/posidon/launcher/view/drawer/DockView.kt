package posidon.launcher.view.drawer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import posidon.launcher.Home
import posidon.launcher.R
import posidon.launcher.items.*
import posidon.launcher.items.users.ItemLongPress
import posidon.launcher.search.SearchActivity
import posidon.launcher.storage.Settings
import posidon.launcher.tools.*
import kotlin.math.abs
import kotlin.math.min

class DockView : LinearLayout {

    fun updateTheme(drawer: View) {

        layoutParams = (layoutParams as MarginLayoutParams).apply {
            val m = Settings["dock:margin_x", 16].dp.toInt()
            leftMargin = m
            rightMargin = m
        }
        val bgColor = Settings["dock:background_color", -0x78000000]
        when (val bgType = Settings["dock:background_type", 0]) {
            0 -> {
                background = null
                containerContainer.background = null
                drawer.background = ShapeDrawable().apply {
                    val tr = Settings["dockradius", 30].dp
                    shape = RoundRectShape(floatArrayOf(tr, tr, tr, tr, 0f, 0f, 0f, 0f), null, null)
                    paint.color = bgColor
                }
            }
            1 -> {
                background = null
                containerContainer.background = null
                drawer.background = LayerDrawable(arrayOf(
                        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(bgColor and 0x00ffffff, bgColor)),
                        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(bgColor,
                                Settings["drawer:background_color", -0x78000000]))
                ))
            }
            2 -> {
                val r = Settings["dockradius", 30].dp
                drawer.background = ShapeDrawable().apply {
                    shape = RoundRectShape(floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f), null, null)
                    paint.color = 0
                }
                containerContainer.background = null
                background = ShapeDrawable().apply {
                    shape = RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)
                    paint.color = bgColor
                }
            }
            3 -> {
                val r = Settings["dockradius", 30].dp
                drawer.background = ShapeDrawable().apply {
                    shape = RoundRectShape(floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f), null, null)
                    paint.color = 0
                }
                background = null
                containerContainer.background = ShapeDrawable().apply {
                    shape = RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)
                    paint.color = bgColor
                }
            }
            else -> {
                Settings["dock:background_type"] = 0
                Log.e("posidon launcher", "dock:background_type can't be $bgType")
            }
        }


        if (!Settings["docksearchbarenabled", false]) {
            searchBar.visibility = GONE
            battery.visibility = GONE
            return
        }
        searchBar.visibility = VISIBLE
        battery.visibility = VISIBLE
        if (Settings["dock:search:below_apps", true]) { searchBar.bringToFront() }
        else { containerContainer.bringToFront() }
        run {
            val color = Settings["docksearchtxtcolor", -0x1000000]
            searchTxt.setTextColor(color)
            searchIcon.imageTintList = ColorStateList.valueOf(color)
            battery.progressTintList = ColorStateList.valueOf(color)
            battery.indeterminateTintMode = PorterDuff.Mode.MULTIPLY
            battery.progressBackgroundTintList = ColorStateList.valueOf(color)
            battery.progressBackgroundTintMode = PorterDuff.Mode.MULTIPLY
            (battery.progressDrawable as LayerDrawable).getDrawable(3).setTint(if (ColorTools.useDarkText(color)) -0x23000000 else -0x11000001)
        }
        searchBar.background = ShapeDrawable().apply {
            val r = Settings["dock:search:radius", 30].dp
            shape = RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)
            paint.color = Settings["docksearchcolor", -0x22000001]
        }
    }

    constructor(c: Context) : super(c)
    constructor(c: Context, a: AttributeSet?) : super(c, a)
    constructor(c: Context, a: AttributeSet?, sa: Int) : super(c, a, sa)

    var dockHeight = 0

    val searchIcon = ImageView(context).apply {
        run {
            val p = 8.dp.toInt()
            setPadding(p, p, p, p)
        }
        setImageResource(R.drawable.ic_search)
        imageTintList = ColorStateList.valueOf(0xffffffff.toInt())
    }

    val searchTxt = TextView(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        textSize = 16f
    }

    val battery = ProgressBar(context, null, R.style.Widget_AppCompat_ProgressBar_Horizontal).apply {
        run {
            val p = 10.dp.toInt()
            setPadding(p, p, p, p)
        }
        max = 100
        progressDrawable = context.getDrawable(R.drawable.battery_bar)
    }

    val searchBar = LinearLayout(context).apply {

        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        setOnClickListener {
            SearchActivity.open(context)
        }

        addView(searchIcon, LayoutParams(56.dp.toInt(), 48.dp.toInt()))
        addView(searchTxt, LayoutParams(0, 48.dp.toInt(), 1f))
        addView(battery, LayoutParams(56.dp.toInt(), 48.dp.toInt()).apply {
            marginEnd = 8.dp.toInt()
        })
    }

    val container = GridLayout(context)

    val containerContainer = FrameLayout(context).apply {
        addView(container, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            this.gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 8.dp.toInt()
            bottomMargin = 8.dp.toInt()
        })
    }

    init {
        gravity = Gravity.CENTER_HORIZONTAL
        orientation = VERTICAL
        addView(searchBar, LayoutParams(MATCH_PARENT, 48.dp.toInt()).apply {
            topMargin = 10.dp.toInt()
            bottomMargin = 10.dp.toInt()
        })
        addView(containerContainer, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    fun loadApps(drawer: DrawerView, feed: View, desktopContent: View, home: Home) {
        val columnCount = Settings["dock:columns", 5]
        val marginX = Settings["dock:margin_x", 16].dp.toInt()
        val appSize = min(when (Settings["dockicsize", 1]) {
            0 -> 64.dp.toInt()
            2 -> 84.dp.toInt()
            else -> 74.dp.toInt()
        }, (Device.displayWidth - marginX * 2) / columnCount)
        val rowCount = Settings["dock:rows", 1]
        val showLabels = Settings["dockLabelsEnabled", false]
        val notifBadgesEnabled = Settings["notif:badges", true]
        val notifBadgesShowNum = Settings["notif:badges:show_num", true]
        container.run {
            removeAllViews()
            this.columnCount = columnCount
            this.rowCount = rowCount
        }
        var i = 0
        while (i < columnCount * rowCount) {
            val view = LayoutInflater.from(context).inflate(R.layout.drawer_item, container, false)
            val img = view.findViewById<ImageView>(R.id.iconimg)
            view.findViewById<View>(R.id.iconFrame).run {
                layoutParams.height = appSize
                layoutParams.width = appSize
            }
            val item = Dock[i]
            val label = view.findViewById<TextView>(R.id.icontxt)
            if (showLabels) {
                label.text = item?.label
                label.setTextColor(Settings["dockLabelColor", -0x11111112])
            } else {
                label.visibility = GONE
            }
            if (item is Folder) {
                img.setImageDrawable(item.icon)
                val badge = view.findViewById<TextView>(R.id.notificationBadge)
                if (notifBadgesEnabled) {
                    val notificationCount = item.calculateNotificationCount()
                    if (notificationCount != 0) {
                        badge.visibility = View.VISIBLE
                        badge.text = if (notifBadgesShowNum) notificationCount.toString() else ""
                        ThemeTools.generateNotificationBadgeBGnFG { bg, fg ->
                            badge.background = bg
                            badge.setTextColor(fg)
                        }
                    } else { badge.visibility = View.GONE }
                } else { badge.visibility = View.GONE }

                val finalI = i
                view.setOnClickListener { item.open(context, view, finalI) }
                view.setOnLongClickListener { item.onLongPress(context, finalI, view); true }
            } else if (item is Shortcut) {
                if (item.isInstalled(context.packageManager)) {
                    img.setImageDrawable(item.icon)
                    view.setOnClickListener {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) item.open(context, it)
                    }
                } else {
                    Dock[i] = null
                    continue
                }
            } else if (item is App) {
                if (!item.isInstalled(context.packageManager)) {
                    Dock[i] = null
                    continue
                }
                val badge = view.findViewById<TextView>(R.id.notificationBadge)
                if (notifBadgesEnabled && item.notificationCount != 0) {
                    badge.visibility = View.VISIBLE
                    badge.text = if (notifBadgesShowNum) item.notificationCount.toString() else ""
                    ThemeTools.generateNotificationBadgeBGnFG(item.icon!!) { bg, fg ->
                        badge.background = bg
                        badge.setTextColor(fg)
                    }
                } else { badge.visibility = View.GONE }
                img.setImageDrawable(item.icon)
                view.setOnClickListener { item.open(context, it) }
                view.setOnLongClickListener { view ->
                    ItemLongPress.showPopupWindow(context, view, item, onRemove = {
                        Dock[i] = null
                        Home.instance.setDock()
                    }, onEdit = null, dockI = i)
                    true
                }
            }
            container.addView(view)
            i++
        }
        val containerHeight = (appSize + if (Settings["dockLabelsEnabled", false]) 18.sp.toInt() else 0) * rowCount
        dockHeight = if (Settings["docksearchbarenabled", false] && !context.isTablet) {
            containerHeight + 84.dp.toInt()
        } else {
            containerHeight + 14.dp.toInt()
        }
        val addition = home.drawerScrollBar.updateTheme(drawer, feed, this)
        dockHeight += addition
        container.layoutParams.height = containerHeight
        setPadding(0, 0, 0, addition)
        drawer.peekHeight = (dockHeight + Tools.navbarHeight + Settings["dockbottompadding", 10].dp).toInt()
        val metrics = DisplayMetrics()
        home.windowManager.defaultDisplay.getRealMetrics(metrics)
        drawer.drawerContent.layoutParams.height = metrics.heightPixels
        (home.homeView.layoutParams as FrameLayout.LayoutParams).topMargin = -dockHeight
        if (Settings["feed:show_behind_dock", false]) {
            (feed.layoutParams as MarginLayoutParams).topMargin = dockHeight
            desktopContent.setPadding(0, 12.dp.toInt(), 0, (dockHeight + Tools.navbarHeight + Settings["dockbottompadding", 10].dp).toInt())
        } else {
            (feed.layoutParams as MarginLayoutParams).run {
                topMargin = dockHeight
                bottomMargin = dockHeight + Tools.navbarHeight + (Settings["dockbottompadding", 10]-18).dp.toInt()
            }
            desktopContent.setPadding(0, 12.dp.toInt(), 0, 24.dp.toInt())
        }
        val bg = drawer.background
        if (Settings["dock:background_type", 0] == 1 && bg is LayerDrawable) {
            bg.setLayerInset(0, 0, 0, 0, Device.displayHeight - Settings["dockbottompadding", 10].dp.toInt())
            bg.setLayerInset(1, 0, drawer.peekHeight, 0, 0)
        }
        (home.findViewById<View>(R.id.blur).layoutParams as CoordinatorLayout.LayoutParams).topMargin = dockHeight
        home.window.decorView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val view = event.localState as View?
                    if (view != null) {
                        val location = IntArray(2)
                        view.getLocationOnScreen(location)
                        val x = abs(event.x - location[0] - view.measuredWidth / 2f)
                        val y = abs(event.y - location[1] - view.measuredHeight / 2f)
                        if (x > view.width / 3.5f || y > view.height / 3.5f) {
                            ItemLongPress.currentPopup?.dismiss()
                            Folder.currentlyOpen?.dismiss()
                            drawer.state = BottomDrawerBehavior.STATE_COLLAPSED
                        }
                    }
                }
                DragEvent.ACTION_DRAG_STARTED -> {
                    (event.localState as View?)?.visibility = INVISIBLE
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    (event.localState as View?)?.visibility = VISIBLE
                    ItemLongPress.currentPopup?.isFocusable = true
                    ItemLongPress.currentPopup?.update()
                }
                DragEvent.ACTION_DROP -> {
                    (event.localState as View?)?.visibility = VISIBLE
                    if (drawer.state != BottomDrawerBehavior.STATE_EXPANDED) {
                        if (event.y > Device.displayHeight - dockHeight) {
                            val item = LauncherItem(event.clipData.description.label.toString())!!
                            val location = IntArray(2)
                            var i = 0
                            while (i < container.childCount) {
                                container.getChildAt(i).getLocationOnScreen(location)
                                val threshHold = min(container.getChildAt(i).height / 2.toFloat(), 100.dp)
                                if (abs(location[0] - (event.x - container.getChildAt(i).height / 2f)) < threshHold && abs(location[1] - (event.y - container.getChildAt(i).height / 2f)) < threshHold) {
                                    Dock.add(item, i)
                                    break
                                }
                                i++
                            }
                        }
                        loadApps(drawer, feed, desktopContent, home)
                    }
                }
            }
            true
        }
    }
}