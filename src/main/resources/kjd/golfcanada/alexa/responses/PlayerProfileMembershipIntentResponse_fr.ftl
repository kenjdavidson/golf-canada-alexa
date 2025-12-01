{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if firstName?? && lastName??>Bonjour ${firstName} ${lastName}. </#if><#if membershipLevel??>Votre niveau d'adhésion est ${membershipLevel}. </#if><#if expirationDate??>Votre adhésion expire le ${expirationDate}. </#if><#if golfCanadaCardId??>Votre numéro de carte Golf Canada est ${golfCanadaCardId}. </#if><#if postHoleByHole??>La saisie trou par trou est <#if postHoleByHole>obligatoire<#else>non obligatoire</#if>.</#if>"
    },
    "shouldEndSession": "false"
}
