@Controller
public class HomeController {
@GetMapping("/")
public String home() {
return "index"; // index.jsp 또는 index.html로 이동
}
}