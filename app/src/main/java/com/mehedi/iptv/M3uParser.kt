package com.mehedi.iptv
object M3uParser {
 fun parse(text:String):List<Channel>{
  val out=mutableListOf<Channel>(); var name:String?=null; var group=""
  text.replace("\r","").lines().forEach{ raw->
   val l=raw.trim()
   if(l.startsWith("#EXTINF",true)){ val c=l.indexOf(','); name=if(c>=0)l.substring(c+1).trim() else "Unknown"; group=Regex("""group-title="([^"]*)""",RegexOption.IGNORE_CASE).find(l)?.groupValues?.getOrNull(1).orEmpty() }
   else if(l.isNotEmpty()&&!l.startsWith("#")&&name!=null){ out+=Channel(out.size+1,name!!,l,group); name=null; group="" }
  }; return out
 }
}
