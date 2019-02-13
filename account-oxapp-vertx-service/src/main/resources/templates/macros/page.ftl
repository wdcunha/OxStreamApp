<#macro page>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${title}</title>
  <link rel="stylesheet" href="/bootstrap/css/bootstrap.min.css">
  <link rel="stylesheet" href="/bootstrap/css/bootstrap-theme.min.css">
  <#if links??>
  ${links}
  </#if>
  <script src="/jquery/jquery.min.js"></script>
  <script src="/bootstrap/js/bootstrap.min.js"></script>
  <#if scripts??>
  ${scripts}
  </#if>
</head>
<body>
    <nav class="navbar navbar-inverse navbar-fixed-top">
      <div class="container-fluid">
        <div class="navbar-header">
          <a class="navbar-brand" href="/">Ox Vert.x Streaming App</a>
        </div>
        <ul class="nav navbar-nav">
          <li class="active"><a href="#">User List</a></li>
        </ul>
        <ul class="nav navbar-nav navbar-right">
          <li><a href="/add_user"><span class="glyphicon glyphicon-user"></span> Sign Up</a></li>
          <li><a href="/login"><span class="glyphicon glyphicon-log-in"></span> Login</a></li>
        </ul>
      </div>
    </nav>
  <#nested>
</body>
</html>
</#macro>