import tkinter as tk
from commands.common.software_command import *

class SoftwarePage(tk.Frame):

   def __init__(self, parent, controller):
      tk.Frame.__init__(self, parent)
      self.device = controller.getDevice()

      self.columnconfigure([0, 1, 2, 3, 4, 5], weight = 1)
      label = tk.Label(self, text = "Install Software", font = ("Courier", 14), anchor = 'center')
      label.grid(row = 0, column = 0, sticky = "nsew", columnspan = 5)

      self.guardian = tk.Label(self, text = "Current guardian version: ", font = ("Courier", 14), anchor = 'center')
      self.guardian.grid(row = 1, column = 0, sticky = "w", columnspan = 4)
      self.admin = tk.Label(self, text = "Current admin version: ", font = ("Courier", 14), anchor = 'center')
      self.admin.grid(row = 2, column = 0, sticky = "w", columnspan = 4)
      self.classify = tk.Label(self, text = "Current classify version: ", font = ("Courier", 14), anchor = 'center')
      self.classify.grid(row = 3, column = 0, sticky = "w", columnspan = 4)
      self.updater = tk.Label(self, text = "Current updater version: ", font = ("Courier", 14), anchor = 'center')
      self.updater.grid(row = 4, column = 0, sticky = "w", columnspan = 4)

      self.guardianButton = tk.Button(self, text = "Install 1.1.5", fg = 'white', bg = "blue", width = 20, height = 2, anchor = 'center')
      self.guardianButton.grid(row = 1, column = 5, sticky = "nsew", columnspan = 5)
      self.adminButton = tk.Button(self, text = "Install 1.1.4", fg = 'white', bg = "blue", width = 20, height = 2, anchor = 'center')
      self.adminButton.grid(row = 2, column = 5, sticky = "nsew", columnspan = 5)
      self.classifyButton = tk.Button(self, text = "Install 1.1.3", fg = 'white', bg = "blue", width = 20, height = 2, anchor = 'center')
      self.classifyButton.grid(row = 3, column = 5, sticky = "nsew", columnspan = 5)
      self.updaterButton = tk.Button(self, text = "Install 1.0.0", fg = 'white', bg = "blue", width = 20, height = 2, anchor = 'center')
      self.updaterButton.grid(row = 4, column = 5, sticky = "nsew", columnspan = 5)

      self.labels = { "guardian": self.guardian, "admin": self.admin, "classify": self.classify, "updater": self.updater }
      self.buttons = { "guardian": self.guardianButton, "admin": self.adminButton, "classify": self.classifyButton, "updater": self.updaterButton }
      self.downloadVersions = { "guardian": "1.1.5", "admin": "1.1.4", "classify": "1.1.3", "updater": "1.0.0" }

      self.refresh()

   def refresh(self):
      softwares = getSoftwares(self.device)
      self.guardian.config(text = f"Current guardian version: {softwares['guardian']}")
      self.admin.config(text = f"Current admin version: {softwares['admin']}")
      self.classify.config(text = f"Current classify version: {softwares['classify']}")
      self.updater.config(text = f"Current updater version: {softwares['updater']}")

      for key in self.labels:
         self.labels[key].config(text = f"Current {key} version: {softwares[key]}")

      for key in self.buttons:
         if softwares[key] == self.downloadVersions[key]:
            self.buttons[key].config(state = "disabled", text = "up to date")
         else:
            self.buttons[key].config(state = "normal", text = f"Install {self.downloadVersions[key]}")
