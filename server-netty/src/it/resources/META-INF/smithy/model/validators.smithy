$version: "2"

metadata validators = [
    {
        name: "ReservedWords",
        id: "ServiceCodenameValidation",
        configuration: {
            reserved: [
                {
                    words: [],
                    reason: "Required validator for trebuchet build",
                }
            ]
        },
    }

    // Rule Description: Avoid using reserved AWS nouns.
    // Refer https://tiny.amazon.com/gwitjo5m/wamazbinviewAWSAPI_Res
    {
        id: "AWSReservedWords"
        name: "ReservedWords"
        namespaces: ["com.amazon.hyperloop.streaming"]
        configuration: {
            reserved: [
                {
                    words: ["*access key*","*account*","*alarm*","*API key*","*Auto Scaling group*",
                            "*Availability Zone*","*Billing*","*bot*","*bucket*","*build*",
                            "*commit*","*Elastic IP address*","*email*","*function*","*grant*",
                            "*identity*","*identity pool*","*identity provider (IdP) *",
                            "*instance*","*integration*","*interconnect*","*load balancer*",
                            "*log event*","*log group*","*log stream*","*method*","*ML model*",
                            "*network interface*","*notification*","*object*","*organization*",
                            "*password*","*permissions*","*region*","*resource group*","*role*",
                            "*security group*","*service-linked role*","*stack*","*subnet*","*tag*",
                            "*tape*","*thing*","*topic*","*usage plan*","*user pool*","*utterance*",
                            "*VPC*","*VPN connection*","*WorkSpace*"]
                    reason: "This is a reserved resource name within AWS. Unless used in the
                    exact context as mention on the following URL, this word cannot be used.
                    Refer https://tiny.amazon.com/gwitjo5m/wamazbinviewAWSAPI_Res"
                }
            ]
        }
    }

]

metadata suppressions = []