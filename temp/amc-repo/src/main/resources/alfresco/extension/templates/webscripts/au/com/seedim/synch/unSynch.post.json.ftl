<#escape x as jsonUtils.encodeJSONString(x)>
{
<#if error??>
	"error" : "${error}"
<#else>

"status" :"${status}"

</#if>
}
</#escape>