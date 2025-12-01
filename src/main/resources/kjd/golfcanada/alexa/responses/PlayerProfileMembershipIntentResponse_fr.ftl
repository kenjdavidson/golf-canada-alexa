{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if firstName?? && lastName??>Bonjour ${firstName} ${lastName}. </#if><#if membershipLevel??>Votre niveau d'adhésion est ${membershipLevel}. </#if><#if golfCanadaCardId??>Votre numéro de carte Golf Canada est ${golfCanadaCardId}. </#if><#if handicap??>Votre handicap est ${handicap}.</#if>"
    },
    "shouldEndSession": "false"
}
