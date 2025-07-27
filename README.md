<h1 align="center">FOX blockchain</h1>
<p align="center"><i>блокчейн для лис.</i></p>
<div align="center">
  <a href="https://github.com/flirsys/foxchain/stargazers">
    <img src="https://img.shields.io/github/stars/https://github.com/flirsys/foxchain" alt="Stars Badge"/>
  </a>
  <a href="https://github.com/flirsys/foxchain/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/flirsys/foxchain?color=2b9348" alt="License Badge"/>
  </a>
</div>
<br>

## Информация
интерфейс ноды:     `https://127.0.0.1:8080`<br/>
интерфейс кошелька: `https://127.0.0.1:8080/wallet.html`

! нода запускается на порту 8080<br/>
(можно изменить в файле src/main/resources/application.properties в параметре blockchain.node.port и )

! работает на https<br/>
(можно изменить удалением всех server.ssl. строк из src/main/resources/application.properties)

присутствует самоподписанный сертификат (src/main/resources/generate.P12)

## License
This project is licensed under the GNU Affero General Public License v3.0. See the [LICENSE](LICENSE) file for details.

<i>Give a ⭐️ if you love this project!</i>