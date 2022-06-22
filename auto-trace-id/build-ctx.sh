echo ${pwd}
cd ../auto-trace-ctx
mvn clean package install -Dmaven.test.skip=true
cp -f ./target/auto-trace-ctx.jar ../auto-trace-id/src/main/resources/lib/