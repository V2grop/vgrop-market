#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p build/test-libs build/test-classes
if [[ ! -f build/test-libs/json.jar ]]; then
 curl --fail --location --retry 2 -o build/test-libs/json.jar https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar
fi
cp="build/test-libs/json.jar:$ANDROID_HOME/platforms/android-35/android.jar"
javac -cp "$cp" -d build/test-classes app/src/main/java/com/v2grop/lbankpulse/*.java build/direct/generated/com/v2grop/lbankpulse/R.java tests/*.java research/*.java
for t in ScenarioEngineTest LocalResearchTest StyleTest LongTermTest DailyFallbackTest AdaptiveTest HandoffTest WatchlistTest FundCalendarTest MarketSearchTest; do
 java -cp "build/test-classes:$cp" "com.v2grop.lbankpulse.$t"
done
python3 -m unittest discover -s research -p 'test_*.py'
