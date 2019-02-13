<#import "macros/page.ftl" as p>

<#assign title in p>Ox Stream App</#assign>
<#assign links in p>
<link href="/css/base.css" rel="stylesheet">
</#assign>

<@p.page>

<div class="jumbotron">
  <div class="container">
    <h1>Users List</h1>
  </div>
</div>


<div class="container">
    <h3></h3>
    <div class="row col-md-12">
        <#list users as user>
            <h2>${user.username}</h2>
        </#list>
    </div>
</div>

</@p.page>
