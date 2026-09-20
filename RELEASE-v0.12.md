# Market Pulse 0.12.0

- گزینه الیوت با دونچیان (روند و شکست کانال) جایگزین شده؛ انتخاب ذخیره‌شده همان جایگاه به دونچیان منتقل می‌شود و نام جدید در تنظیمات مشخص است.
- ماهانه: افق ۳۰ روز؛ حداقل ۱۸۰ کندل روزانه. سه‌ماهه: ۹۰ روز؛ حداقل ۳۶۵ کندل روزانه. هر دو پیش‌فرض در بخش بسته قرار دارند.
- موتور اصلی کوتاه‌مدت حفظ شده است. بخش بلندمدت سبک اصلی از EMA روزانه ۵۰/۲۰۰، مومنتوم ۳۰/۹۰ و نسبت حرکت خالص به مسیر حرکت استفاده می‌کند. این یک روش ابتکاری مستقل است، نه تبدیل افق ساعتی به روزانه.
- دونچیان از سقف/کف کندل‌های قبلی (بدون کندل آخر)، موقعیت قیمت و EMA۲۰/۵۰ استفاده می‌کند. پنجره‌ها برای ۴ساعت/روز/هفته/ماه/سه‌ماه: ۲۰/۵۵/۱۶۸/۵۵/۹۰ کندل؛ سه افق اول با کندل ساعتی و دو افق آخر با کندل روزانه.
- SMC ماهانه از پنجره ۵۵ روز و سه‌ماهه از ۱۶۸ روز استفاده می‌کند. دفتر سفارش فقط ۴ساعته و معاملات آدرس‌های منتخب حداکثر هفتگی هستند؛ در بلندمدت حذف و علت ثبت می‌شود.
- میانگین برابر سبک‌های دارای داده همچنان روش ترکیب است. درصدها وزن سناریوی آزمایشی هستند؛ بک‌تست یا کالیبراسیون احتمال انجام نشده و خبر/فاندامنتال در فرمول درصد وارد نشده‌اند.
- کندل روزانه از Binance سپس Bybit دریافت می‌شود. LBank برای نقدی و مرجع جایگزین XAUT در دسترس است. منابع ناموفق، تاریخچه دارای شکاف و داده قدیمی خروجی عددی نمی‌گیرند. برای XAUT نوع نقدی/دائمی و مبنای جایگزین صریح نمایش داده می‌شود.
- کارت بالای تحلیل: قیمت با نام صرافی، ارز مظنه، نقدی/دائمی، مارک/آخرین معامله و ساعت دریافت. تازه‌سازی ۳۰ ثانیه بعد از پایان درخواست، فقط وقتی صفحه باز و اپ در پیش‌زمینه است؛ اتصال تیک‌به‌تیک نیست. شکست درخواست قیمت را ناموجود نمایش می‌دهد. زمان نمایش‌داده‌شده زمان دریافت است، نه الزاماً زمان آخرین معامله در صرافی.
- برای بازار نقدی، قیمت LBank اولویت دارد؛ برای نمادهای شناخته‌شده، منابع Binance/Bybit/Hyperliquid با برچسب واقعی استفاده می‌شوند. قیمت نقدی XAUT به عنوان قیمت قرارداد دائمی معرفی نمی‌شود.

## Reference
https://www.tradingview.com/support/solutions/43000502253-donchian-channels-dc/
Only channel definition is sourced; scoring and combination weights are application heuristics, not an endorsed trading system.

## Validation
APK compiled against Android 35, min SDK31, using JDK17 and the prior debug signing key. Automated tests cover original engine parity, daily history minimums/gaps/staleness, directional scenarios and sums, selected-style combination, quote identity, handoff, favorites and calendar parsing. No emulator/device UI or background lifecycle test was available; install on Android for final device verification.

Build: set JAVA_HOME to JDK17 and ANDROID_HOME to SDK with platform35 and build-tools35.0.1, then run bash build-direct.sh.
