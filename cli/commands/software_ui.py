import tkinter as tk
from commands.common.software_command import *

class SoftwarePage(tk.Frame):

   def __init__(self, parent, controller):
      tk.Frame.__init__(self, parent)
      self.device = controller.getDevice()

      self.columnconfigure([0, 1, 2, 3, 4, 5, 6], weight = 1)
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

      self.guardianButton = tk.Button(self, text = "Install 1.1.7", fg = 'black', bg = "blue", width = 20, height = 2, anchor = 'center', command = lambda: self.install("guardian"))
      self.guardianButton.grid(row = 1, column = 5, sticky = "nsew")
      self.adminButton = tk.Button(self, text = "Install 1.1.7", fg = 'black', bg = "blue", width = 20, height = 2, anchor = 'center', command = lambda: self.install("admin"))
      self.adminButton.grid(row = 2, column = 5, sticky = "nsew")
      self.classifyButton = tk.Button(self, text = "Install 1.1.4", fg = 'black', bg = "blue", width = 20, height = 2, anchor = 'center', command = lambda: self.install("classify"))
      self.classifyButton.grid(row = 3, column = 5, sticky = "nsew")
      self.updaterButton = tk.Button(self, text = "Install 1.0.0", fg = 'black', bg = "blue", width = 20, height = 2, anchor = 'center', command = lambda: self.install("updater"))
      self.updaterButton.grid(row = 4, column = 5, sticky = "nsew")

      self.guardianDButton = tk.Button(self, text = "Downgrade to 1.1.6", fg = 'black', bg = "red", width = 20, height = 2, anchor = 'center', command = lambda: self.downgrade("guardian"))
      self.guardianDButton.grid(row = 1, column = 6, sticky = "nsew")
      self.adminDButton = tk.Button(self, text = "Downgrade to 1.1.6", fg = 'black', bg = "red", width = 20, height = 2, anchor = 'center', command = lambda: self.downgrade("admin"))
      self.adminDButton.grid(row = 2, column = 6, sticky = "nsew")
      self.classifyDButton = tk.Button(self, text = "Downgrade to 1.1.3", fg = 'black', bg = "red", width = 20, height = 2, anchor = 'center', command = lambda: self.downgrade("classify"))
      self.classifyDButton.grid(row = 3, column = 6, sticky = "nsew")
      self.updaterDButton = tk.Button(self, text = "Downgrade to 0.9.0", fg = 'black', bg = "red", width = 20, height = 2, anchor = 'center', command = lambda: self.downgrade("updater"))
      self.updaterDButton.grid(row = 4, column = 6, sticky = "nsew")

      self.labels = { "guardian": self.guardian, "admin": self.admin, "classify": self.classify, "updater": self.updater }
      self.downloadButtons = { "guardian": self.guardianButton, "admin": self.adminButton, "classify": self.classifyButton, "updater": self.updaterButton }
      self.downgradeButtons = { "guardian": self.guardianDButton, "admin": self.adminDButton, "classify": self.classifyDButton, "updater": self.updaterDButton }
      self.downloadVersions = { "guardian": "1.1.7", "admin": "1.1.7", "classify": "1.1.4", "updater": "1.0.0" }
      self.downgradeVersions = { "guardian": "1.1.6", "admin": "1.1.6", "classify": "1.1.3", "updater": "0.9.0" }

      self.refresh()

   def install(self, role):
      downloadSoftwares(self.device, role)
      self.refresh()

   def downgrade(self, role):
      downgradeSoftwares(self.device, role)
      self.refresh()

   def refresh(self):
      softwares = getSoftwares(self.device)
      self.guardian.config(text = f"Current guardian version: {softwares['guardian']}")
      self.admin.config(text = f"Current admin version: {softwares['admin']}")
      self.classify.config(text = f"Current classify version: {softwares['classify']}")
      self.updater.config(text = f"Current updater version: {softwares['updater']}")

      for key in self.labels:
         self.labels[key].config(text = f"Current {key} version: {softwares[key]}")

      for key in self.downloadButtons:
         if softwares[key] >= self.downloadVersions[key]:
            self.downloadButtons[key].config(state = "disabled", text = "Up to date")
         else:
            self.downloadButtons[key].config(state = "normal", text = f"Install {self.downloadVersions[key]}")

      for key in self.downgradeButtons:
         if softwares[key] == self.downgradeVersions[key]:
            self.downgradeButtons[key].config(state = "disabled", text = "Lowest version available")
         else:
            self.downgradeButtons[key].config(state = "normal", text = f"Downgrade to {self.downgradeVersions[key]}")
