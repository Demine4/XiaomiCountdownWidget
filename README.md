# 小米日历倒数日小部件

## 当前版本 0.17.0

三列列表统一隐藏滚动条，避免返回桌面时滚动条短暂出现；仍然可以上下滑动查看全部内容。直接覆盖安装 outputs/XiaomiCountdown-0.17.0.apk。

统一点击打开日历：空列整块区域以及短列表下方留白也绑定日历入口。移除纪念日旁容易误解的刷新箭头，手动刷新留在应用内。见 outputs/0.16.0点击区域说明.md。

Android 12及以上恢复固定三列，每列独立上下滚动，标题固定，显示全部记录。纪念日以名称、累计天数、起始日期和下次周年分行显示，长内容自动换行。桌面滚动手势和点击仍需小米真机确认。见 outputs/0.15.0三列滚动说明.md。

光晕支持任选1～4个颜色，使用RGB滑块或输入HEX色值，带背景预览和恢复默认。背景明暗度只调整光晕底色，调到0/100仍保留光晕。见 outputs/0.14.0光晕配色说明.md。

直接安装的 Android 原生桌面小部件，不需要设计师账号、上架、Root 或主题导入。包名和本地签名沿用 0.1.0 检测版，可以覆盖安装。

目标：沿用小米日历的数据，自动隐藏过期的一次性倒数日，原日历数据完整保留。半透明浅色圆角卡片，三列为倒数日、纪念日、周期倒数日；第三列可切换为生日。行内显示名称与天数（生日显示月日），虚线分隔，可调整透明度、字号、每列行数。应用内预览与桌面共用WidgetRenderer。

最新分区布局见 outputs/0.13.0通栏纪念日说明.md；纪念日详情和周期归类见 outputs/0.12.0纪念日与归类说明.md；每列最多15行见 outputs/0.11.0更新说明.md；自定义图片见 outputs/0.6.0背景设置说明.md；系统授权见 outputs/0.4.0更新与授权说明.md。可覆盖安装旧版本。

## 已确认的数据接口

用户在 Android 16 / API 36、澎湃 OS 3、小米日历 17.10.8.21 真机上确认：

- READ_CALENDAR 可以读取 CalendarContract.Events，以及指定记录的 ExtendedProperties。
- 一条倒数日的 hasExtendedProperties 为9，具有名为 key_countdown_info 的 JSON 扩展属性。
- JSON包含 timeMillis（目标日期毫秒时间戳）和 repeatType。
- 用户额外确认 repeatType=0 不重复、1 每月重复、2 每年重复。
- 用户提供生日样例：key_birthday_info，firstBirthMillis/name/dateType=0；纪念日样例：key_anniversary_info，timeMillis/content/dateType=0。0.3.0接入这两类字段，其他日期类型跳过并提示。
- 0.5.0用户提供农历生日样例：dateType=1，Events.rdate是小米日历生成的YYYYMMDD实例列表。RecurrenceDates直接读取该列表并排除exdate，选择下一次日期；LunarLabels用Android ICU按Asia/Shanghai生成农历月日标签。未提供未来实例时会提示，不自行猜算。

正式数据层按扩展属性名识别倒数日，不单凭类型值9猜测。一次性记录过期隐藏；重复记录计算不早于今天的下一次日期，按日期排序；普通日程不显示。公历日期按手机当前时区解释。每月31日遇短月、每年2月29日遇非闰年，采用月末补齐规则，再次进入长月/闰年恢复原始日期。农历重复项目使用rdate，不沿用公历重复规则。未知类型、缺失或损坏字段跳过并提示。

倒数日与三列主功能已获用户真机确认。纪念日显示从原始日期起经过的自然日天数，生日按下一次日期排序并显示月日。0.5.0补齐视频中缺失的农历生日，新增部分仍需真机核对。

## 结构

- HomeActivity：权限、添加桌面、样式设置、倒数日预览及过期记录查看。
- CountdownStore：仅查询系统日历及倒数日、纪念日、生日扩展属性；无写入、删除调用。
- WidgetRenderer：桌面和应用预览共用的三列表格RemoteViews。
- 0.13.0默认有纪念日时使用SectionRenderer上下分区，上方两列、下方纪念日通栏，按文本测量及SectionLayout的高度预算分配显示条数；可关闭wide_anniversaries恢复原布局。没有纪念日时保留三列。
- WidgetStyle：纯色/渐变/自定义背景、深浅模式、文字配色、明暗透明度、分隔线和网格装饰；标题与日期字号可分开调整。点击卡片使用CalendarNavigation打开小米日历，设置可从应用图标或桌面支持的长按编辑入口进入。
- 0.6.0将渐变改为真正的水平方向粉紫蓝到白色过渡，增加独立色彩强度；PhotoBackground通过系统选择器导入长边最多1600像素的私有图片副本，支持圆角铺满、100–300%缩放、水平/垂直位置及移除。ImagePlacement为可单独测试的裁切几何，不额外申请相册/存储权限。
- 0.7.0按用户进一步明确的视觉要求，将色带替换成HaloBackground多中心柔光云团：粉紫聚于左上、浅蓝散于左下，向周围自然淡出并保留右侧白色。像素绘制与JVM预览共享代码，已生成并检查背景预览。
- 0.8.0进一步缩小云团、减少重叠并加快消散，新增范围和消散速度滑块。预览仍与Android共用像素绘制代码。
- CountdownRules：无 Android 依赖的自然日及重复日期计算。
- AnniversaryRules：保留原始日期计算累计天数、下一次周年和结婚文案。纪念日采用独立两行布局；周期归类使用本地anniversary_ids逐条选择，转换前保留所有周期候选，选中项目只在纪念日栏展示，不修改系统日历。
- CountdownWidget：RemoteViews，按组件尺寸自动减少显示行数，手动刷新、周期刷新及次日非精确刷新。
- CalendarWatch：日历变更触发的 JobScheduler 任务，无常驻服务。系统可能延迟调度。
- 0.4.0增加独立的可持久化周期任务（30分钟）、桌面添加确认回调、自适应/单色图标，以及小米桌面权限、自启动、省电策略与Android电池优化设置入口。私有设置页面均作尽力跳转，失败退回应用详情；不把打开设置页面当作授权成功。
- MainActivity：保留原始字段检测工具；默认报告省略内容，复制完整字段前再次确认。

声明读取日历、接收开机广播、旧式桌面快捷方式创建和请求电池优化豁免权限。后两者用于系统授权引导，不替代小米系统开关。没有网络、写入日历、整个相册访问或精确闹钟权限；无第三方SDK。样式、添加回调与刷新时间保存在本机 SharedPreferences；选中的背景图副本保存在应用私有目录；事件内容不写入应用文件。

## 构建与验证

./build-local.ps1 使用已有 Android SDK 36、Build Tools 36.1.0、Android Studio JDK 21 构建APK、检查签名与对齐。资源通过 AAPT2 编译，Java通过javac/D8编译，无需下载 Gradle 依赖。可通过脚本参数覆盖工具路径。

本地签名文件：work/signing/countdown-local.jks。务必保留，以便后续覆盖更新。证书密码写在构建脚本中，仅适合个人开发；不要公开该证书，不要提交到Git。

Android Studio 也可以导入标准AGP 8.13.0项目，需要Gradle 8.13与SDK36，未附Gradle wrapper。Gradle调试签名与手动构建签名不同，勿混用覆盖安装。

./test-local.ps1 执行57项检查：20项日期边界、10项rdate选择、9项照片裁切、12项纪念日、6项分区空间分配检查（含183组高度和字号组合）。APK编译、签名、权限清单和小部件声明已检查。新增分区排版仍需小米真机核对。

tests/android/WidgetSmokeInstrumentation.java 为真实Android上的RemoteViews加载及截图测试，使用合成数据，独立测试APK不包含在交付应用中。

## 参考

- [小米日历字段文档](https://zhuti.designer.xiaomi.com/docs/grammar/)
- [Android Calendar Provider](https://developer.android.com/identity/providers/calendar-provider)
- [Android小部件刷新](https://developer.android.com/develop/ui/views/appwidgets/advanced)
- [小米原生小部件无需审核说明](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1588)
