#How to update gitea.json
1. Download gitea from https://gitea.your.host/swagger.v1.json
2. Remove everything from securityDefinitions and security (because otherwise the restclient will add multiple values to the Authorization header) except:

	"securityDefinitions": {
	    "AuthorizationHeaderToken": {
	      "description": "API tokens must be prepended with \"token\" followed by a space.",
	      "type": "apiKey",
	      "name": "Authorization",
	      "in": "header"
	    }
	  },
	  "security": [
	    {
	      "AuthorizationHeaderToken": []
	    }
	  ]