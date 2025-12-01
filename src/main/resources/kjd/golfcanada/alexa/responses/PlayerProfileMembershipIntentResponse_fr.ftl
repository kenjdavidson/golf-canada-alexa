{
    "outputSpeech": {
        "type": "PlainText",
        "text": "Bonjour ${firstName} ${lastName}. <#if membershipLevel?has_content>Votre niveau d'adhésion est ${membershipLevel}. </#if><#if golfCanadaCardId?has_content>Votre numéro de carte Golf Canada est ${golfCanadaCardId}. </#if><#if handicap?has_content>Votre handicap est ${handicap}.</#if>"
    },
    "shouldEndSession": "false"
}
