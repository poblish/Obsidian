# Run with browsers obtained from a locally-running Selenium Grid
java -jar dist/obsidian.jar --nogui --nosocketio \
  --seleniumGridUrl=http://${1}/wd/hub \
  ${@:2}
