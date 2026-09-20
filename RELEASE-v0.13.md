# VGrop market 0.13.0

نام نمایش اپ VGrop market است. شناسه بسته و کلید امضای نسخه قبلی حفظ شده تا ارتقا روی نصب قبلی ممکن باشد.
سربرگ سمت راست: «کاری از vgrop» و t.me/V2grop. لمس آن Telegram را با tg://resolve?domain=V2grop باز می‌کند؛ در نبود برنامه، لینک HTTPS باز می‌شود.
لوگو: نشان برداری VG با فلش رشد، بنفش و فیروزه‌ای. نسخه سازگار با ماسک آیکن Android نیز اضافه شده است. فایل brand-logo.svg داخل سورس موجود است.

## سبک بلندمدت
گزینه مستقل «سبک بلندمدت • دنبال‌کردن روند ۵۰/۲۰۰» پیش‌فرض روشن است. در حالت روشن، افق ۳۰ و ۹۰روزه با این موتور محاسبه می‌شود و تیک‌های سبک کوتاه‌مدت روی آن اثر نمی‌گذارند. در حالت خاموش، رفتار ترکیب سبک‌های انتخابی نسخه ۰.۱۲ برقرار است. افق‌های کوتاه‌تر حفظ شده‌اند.

روش بلندمدت از نسخه ۰.۱۲ جدا و صریح شده: EMA روزانه۵۰/۲۰۰، تغییر لگاریتمی۳۰/۹۰روزه، نسبت تغییر خالص قیمت به مجموع تغییرات مطلق. حداقل۲۰۰ کندل برای یک‌ماهه و۳۶۵ برای سه‌ماهه. خنثی‌بودن، بازار کم‌جهت و نبود تاریخچه همچنان مجازند؛ هدف تولید جهت اجباری نیست. درصدها وزن آزمایشی هستند، نه احتمال معتبر یا تضمین سود.
مرجع مفهوم میانگین‌ها: https://www.fidelity.com/viewpoints/active-investor/moving-averages
فرمول وزن‌دهی اختصاصی است و مورد تأیید Fidelity یا بک‌تست‌شده نیست.

## دریافت روزانه
Binance، Bybit، Hyperliquid (قرارداد دائمی و نماد دقیق) و LBank نقدی هم‌زمان با مهلت۲۲ثانیه بررسی می‌شوند. نخستین پاسخ کامل >=۳۶۵ کندل از نوع بازار درخواستی قابل انتخاب است؛ در غیر این صورت بهترین پوشش واجد شرایط انتخاب می‌شود. در نبود تاریخچه دائمی، مرجع نقدی شناخته‌شده با برچسب صریح استفاده می‌شود. نمودارها بین منابع به هم چسبانده نمی‌شوند.
نمادهای ناشناخته بین صرافی‌ها تطبیق داده نمی‌شوند. شکاف، کهنگی و ورودی نامعتبر رد می‌شوند. USDT/USDC و نقدی/دائمی کنار منبع مشخص است. دارایی تازه‌فهرست‌شده همچنان ممکن است تاریخچه کافی نداشته باشد.
مرجع API: https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/info-endpoint

## Verification
APK compiled and debug-signed with previous key; minSDK31, target35. Automated tests cover independent long-style behavior with short-only selections, Hyperliquid daily parsing, labeled spot fallback, history thresholds, original scenario parity, sums and source failure. Vector logo inspected at512px. No Android emulator/device UI test performed; link launching and launcher mask depend on device handlers.
