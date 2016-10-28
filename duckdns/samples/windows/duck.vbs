Call LogEntry()

Sub LogEntry()
	On Error Resume Next
	Dim objRequest
	Dim URL

	URL = "https://www.duckdns.org/update?domains=example&token=489365461-95d3-31c8-ade8-865c0ee9de3d&ip="

	Set objRequest = CreateObject("Microsoft.XMLHTTP")
	objRequest.open "GET", URL , false
	objRequest.Send
	Set objRequest = Nothing
End Sub