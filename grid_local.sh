# Run with browsers obtained from a locally-running Selenium Grid
java -jar dist/obsidian.jar --nogui --nosocketio \
  --seleniumGridUrl=http://localhost:4444/wd/hub \
  $@
