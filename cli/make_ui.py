import tkinter as tk
import commands.menu as menu_ui
import commands.registration_ui as registration_ui
import commands.software_ui as software_ui
import commands.classifier_ui as classifier_ui
import commands.diagnostics_ui as diagnostics_ui
from commands.adb import Device

class Application(tk.Tk):

    def __init__(self, *args, **kwargs):
      tk.Tk.__init__(self, *args, **kwargs)

      container = tk.Frame(self)
      container.pack(side = "top", fill = "both", expand=True)
      container.grid_columnconfigure([0, 1], weight = 1)

      self.device = Device()

      self.frames = {}

      for F in (menu_ui.MenuPage, registration_ui.RemoveRegistrationPage, software_ui.SoftwarePage, classifier_ui.RemoveClassifierPage, diagnostics_ui.Diagnostics):
        frame = F(container, self)
        self.frames[F] = frame

      menuFrame = self.frames[menu_ui.MenuPage]
      menuFrame.grid(row = 0, column = 0, sticky = "nsew")
      menuFrame.tkraise()

    def show_frame(self, cont):
        frame = self.frames[cont]
        frame.grid(row = 0, column = 1, sticky ="nsew")
        frame.tkraise()

    def getDevice(self):
        return self.device

if __name__ == "__main__":
    app = Application()
    app.mainloop()
    