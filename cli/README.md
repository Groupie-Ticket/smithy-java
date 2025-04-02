## smithy-call

This module contains the base-version of smithy-call: a CLI that uses ahead-of-time compilation and the dynamic client to make adhoc calls to services.


The functionality provided by this CLI includes:
1. Execute operations listed in a service model
2. List operations listed in a service model
3. SigV4 authentication
4. Multi-protocol support

### Example Call
1. Build the native binary for smithy-call: `./gradlew :cli:nativeCompile`
2. Start-up Cafe service from the end-to-end example in a separate window: `./gradlew :examples:end-to-end:run`
3. Check the available operations: `./cli/build/native/nativeCompile/smithy-call com.example#CoffeeShop --list-operations -m /Users/fluu/workplace/smithy-java-cli/examples/end-to-end/model`
4. On the command line, export your AWS access key ID and secret access key via `export AWS_ACCESS_KEY_ID=[YOUR_AWS_ACCESS_KEY_ID]` and `export AWS_SECRET_ACCESS_KEY=[YOUR_AWS_SECRET_ACCESS_KEY]`
5. Now, we are ready to send requests to an AWS-Auth enabled service.
6. Send a menu request to our Cafe service: `./cli/build/native/nativeCompile/smithy-call com.example#CoffeeShop GetMenu -m /Users/fluu/workplace/smithy-java-cli/examples/end-to-end/model --url http://localhost:8888 --auth aws --aws-region us-east-1`
7. Send an order request to our Cafe service: `./cli/build/native/nativeCompile/smithy-call com.example#CoffeeShop CreateOrder -m /Users/fluu/workplace/smithy-java-cli/examples/end-to-end/model --url http://localhost:8888 --input-json '{"coffeeType": "DRIP"}' --auth aws --aws-region us-east-1`
8. Make sure to note down the "id" field returned by the call, as you will be getting your order using this unique order id.
9. Get your order via: `./cli/build/native/nativeCompile/smithy-call com.example#CoffeeShop GetOrder -m /Users/fluu/workplace/smithy-java-cli/examples/end-to-end/model --url http://localhost:8888 --input-json '{"id": "[YOUR_ORDER_ID]"}' --auth aws --aws-region us-east-1`

